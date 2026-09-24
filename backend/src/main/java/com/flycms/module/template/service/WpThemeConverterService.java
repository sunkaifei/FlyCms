package com.flycms.module.template.service;

import com.alibaba.fastjson.JSON;
import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * WordPress 主题包 → FlyCms 皮肤 转换器（D22）。
 *
 * <p><b>定位</b>：让用户把现成的 WordPress 主题 zip 直接上传，系统自动转成
 * 本 CMS 可渲染的 FreeMarker 皮肤，再基于产物派生出栏目/模型专属模板。
 * 解决"从零写一套皮肤成本过高"的起步难题。
 *
 * <p><b>这是有损转换</b>：HTML/CSS/JS 100% 保留；The Loop 与常用内容函数能转；
 * 菜单、小部件、自定义字段、评论、插件函数、FSE 区块主题无法自动映射，
 * 一律转成 TODO 标记留在模板里，并在报告里汇总——<b>绝不静默丢弃</b>。
 *
 * <p><b>安全</b>：与 {@link TemplateCenterService#importSkin} 同一套防御——
 * zip-slip（canonical 前缀校验）、后缀白名单、大小上限、临时目录用完即删。
 *
 * <p><b>产出落位</b>（关键）：FlyCms 的模板与静态资源是<b>两个</b>目录，
 * 转换器自动分别落位，转换完成即可切换使用，无需手工搬运：
 * <ul>
 *   <li>模板 → {@code views/templates/pc_theme/{skin}/}</li>
 *   <li>资源 → {@code views/static/assets/skin/pc_theme/{skin}/}</li>
 * </ul>
 *
 * <p><b>与 tools/wp-theme-converter/convert.py 的关系</b>：Python 版是离线命令行工具
 * （便于服务器上批量转换），本类是产品内实现。<b>改动映射表时两边需同步</b>。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class WpThemeConverterService {

    /** 模板根（与 TemplateCenterService.THEME_ROOT 一致） */
    public static final String TEMPLATE_ROOT = "views/templates/pc_theme";
    /** 静态资源根：模板里 ${skinPath} 指向此处 */
    public static final String ASSET_ROOT = "views/static/assets/skin/pc_theme";

    /** 皮肤名白名单 */
    private static final Pattern SKIN_NAME = Pattern.compile("^[A-Za-z0-9_\\-]{1,50}$");

    /** 允许带进皮肤的资源后缀（与 TemplateCenterService.PACK_EXT 同思路） */
    private static final List<String> PACK_EXT = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".md", ".txt",
            ".jpg", ".jpeg", ".png", ".gif", ".svg", ".ico", ".webp",
            ".woff", ".woff2", ".ttf", ".eot");

    /**
     * 解压白名单 = 资源白名单 + php。
     *
     * <p><b>为什么 php 必须放行</b>：WP 主题模板本身就是 php，不放行等于什么都转不了。
     * 安全边界不在这里——php <b>只会作为输入被读取</b>，产物一律是 {@code .html}
     * （FILE_MAP 的值是硬编码的），且资源复制另有 {@link #PACK_EXT} 过滤，
     * 因此 php 绝不会出现在最终皮肤目录里。
     */
    private static final List<String> UNZIP_EXT = new ArrayList<>(PACK_EXT);
    static {
        UNZIP_EXT.add(".php");
    }

    /** 主题包大小上限 50MB */
    public static final long MAX_PACK_BYTES = 50L * 1024 * 1024;

    // /////////////////// 文件映射 ///////////////////

    /** WP 模板文件 → FlyCms 皮肤内相对路径（LinkedHashMap 保证顺序：靠前的优先占用目标名） */
    private static final Map<String, String> FILE_MAP = new LinkedHashMap<>();
    static {
        FILE_MAP.put("index.php", "index.html");
        FILE_MAP.put("front-page.php", "index.html");
        FILE_MAP.put("home.php", "index.html");
        FILE_MAP.put("single.php", "detail.html");
        FILE_MAP.put("page.php", "page.html");
        FILE_MAP.put("archive.php", "list.html");
        FILE_MAP.put("category.php", "list.html");
        FILE_MAP.put("tag.php", "tag.html");
        FILE_MAP.put("search.php", "search.html");
        FILE_MAP.put("404.php", "404.html");
        FILE_MAP.put("header.php", "parts/header.html");
        FILE_MAP.put("footer.php", "parts/footer.html");
        FILE_MAP.put("sidebar.php", "parts/sidebar.html");
    }

    /** 明确不转换的文件 → 原因（写进报告） */
    private static final Map<String, String> SKIP_FILES = new LinkedHashMap<>();
    static {
        SKIP_FILES.put("functions.php",
                "主题功能注册（钩子/小部件/菜单），FlyCms 无对应机制，需按需求用标签或碎片重写");
        SKIP_FILES.put("comments.php",
                "评论模板，FlyCms 评论体系与 WP 不同，请用文章评论标签重写");
        SKIP_FILES.put("searchform.php",
                "搜索表单，用纯 HTML form 提交到 /search 即可");
    }

    /** 页面类型：决定是否走 The Loop 转换、用 item 还是 info */
    private static final Map<String, String> PAGE_TYPE = new LinkedHashMap<>();
    static {
        // WP 的 index.php 就是文章列表（The Loop），必须按 list 处理
        PAGE_TYPE.put("index.html", "list");
        PAGE_TYPE.put("list.html", "list");
        PAGE_TYPE.put("tag.html", "list");
        PAGE_TYPE.put("search.html", "list");
        PAGE_TYPE.put("detail.html", "detail");
        PAGE_TYPE.put("page.html", "detail");
        PAGE_TYPE.put("404.html", "index");
    }

    /** 原样复制的资源目录 */
    private static final List<String> RESOURCE_DIRS = Arrays.asList(
            "css", "js", "images", "img", "assets", "fonts");

    // /////////////////// 函数映射 ///////////////////

    /**
     * 构造"匹配整个 PHP 标签"的正则：{@code <?php [echo] call([arg])[;] ?>}。
     *
     * <p><b>为什么必须匹配整个标签</b>：只匹配函数名会留下 {@code <?php ... ?>} 空壳，
     * 被 {@link #stripResidualPhp} 兜底再包一层 TODO，产出嵌套注释。
     */
    private static String php(String call) {
        return php(call, null);
    }

    private static String php(String call, String arg) {
        String argPart = (arg == null) ? "[^)]*\\s*" : Pattern.quote(arg) + "\\s*";
        return "<\\?php\\s*(?:echo\\s+)?" + call + "\\s*\\(\\s*" + argPart + "\\)\\s*;?\\s*\\?>";
    }

    /** 列表循环体内的函数（数据变量用 item） */
    private static final List<String[]> LOOP_MAP = new ArrayList<>();
    static {
        LOOP_MAP.add(new String[]{php("the_time", "'Y-m-d'"), "${(item.createTime?substring(0,10))!''}"});
        LOOP_MAP.add(new String[]{php("the_time", "\"Y-m-d\""), "${(item.createTime?substring(0,10))!''}"});
        LOOP_MAP.add(new String[]{php("the_time", "'Y'"), "${(item.createTime?substring(0,4))!''}"});
        LOOP_MAP.add(new String[]{php("the_time"), "${(item.createTime)!''}"});
        LOOP_MAP.add(new String[]{php("the_date"), "${(item.createTime?substring(0,10))!''}"});
        LOOP_MAP.add(new String[]{php("the_title"), "${(item.title)!''}"});
        LOOP_MAP.add(new String[]{php("get_the_title"), "${(item.title)!''}"});
        LOOP_MAP.add(new String[]{php("the_permalink"), "/${model.code}/${item.shortUrl}.html"});
        LOOP_MAP.add(new String[]{php("get_permalink"), "/${model.code}/${item.shortUrl}.html"});
        LOOP_MAP.add(new String[]{php("the_content"), "${(item.content)!''}"});
        LOOP_MAP.add(new String[]{php("the_excerpt"), "${(item.description)!''}"});
        LOOP_MAP.add(new String[]{php("the_ID"), "${item.id!''}"});
        LOOP_MAP.add(new String[]{php("get_the_ID"), "${item.id!''}"});
        LOOP_MAP.add(new String[]{php("the_author"), "${(item.author)!''}"});
        LOOP_MAP.add(new String[]{php("the_post_thumbnail_url"), "${(item.image)!''}"});
        LOOP_MAP.add(new String[]{php("the_post_thumbnail"),
                "<img src=\"${(item.image)!''}\" alt=\"${(item.title)!''}\">"});
    }

    /** 详情页的函数（数据变量用 info） */
    private static final List<String[]> SINGLE_MAP = new ArrayList<>();
    static {
        SINGLE_MAP.add(new String[]{php("the_time", "'Y-m-d'"), "${(info.createTime?substring(0,10))!''}"});
        SINGLE_MAP.add(new String[]{php("the_time"), "${(info.createTime)!''}"});
        SINGLE_MAP.add(new String[]{php("the_title"), "${(info.title)!''}"});
        SINGLE_MAP.add(new String[]{php("get_the_title"), "${(info.title)!''}"});
        SINGLE_MAP.add(new String[]{php("the_permalink"), "/${model.code}/${info.shortUrl}.html"});
        SINGLE_MAP.add(new String[]{php("the_content"), "${(info.content)!''}"});
        SINGLE_MAP.add(new String[]{php("the_excerpt"), "${(info.description)!''}"});
        SINGLE_MAP.add(new String[]{php("the_ID"), "${info.id!''}"});
        SINGLE_MAP.add(new String[]{php("the_author"), "${(info.author)!''}"});
        SINGLE_MAP.add(new String[]{php("the_post_thumbnail"),
                "<img src=\"${(info.image)!''}\" alt=\"${(info.title)!''}\">"});
    }

    /** 全局函数（任何模板都适用） */
    private static final List<String[]> GLOBAL_MAP = new ArrayList<>();
    static {
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_header\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "<@fly_part name=\"header\"/>"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_footer\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "<@fly_part name=\"footer\"/>"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_sidebar\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "<@fly_part name=\"sidebar\"/>"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*wp_head\\s*\\(\\s*\\)\\s*;?\\s*\\?>",
                "<!-- WP: wp_head() 钩子，FlyCms 无需，样式/脚本直接在模板引入 -->"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*wp_footer\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "<!-- WP: wp_footer() 钩子，FlyCms 无需 -->"});
        GLOBAL_MAP.add(new String[]{"<\\?php\\s*wp_body_open\\s*\\(\\s*\\)\\s*;?\\s*\\?>", ""});
        GLOBAL_MAP.add(new String[]{"<\\?php\\s*body_class\\s*\\(\\s*\\)\\s*;?\\s*\\?>", ""});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*language_attributes\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "lang=\"zh\""});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_template_directory_uri\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "${skinPath}"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_stylesheet_directory_uri\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "${skinPath}"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?get_stylesheet_uri\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "${skinPath}/css/style.css"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:esc_url\\s*\\(\\s*)?home_url\\s*\\(\\s*\\)\\s*\\)?\\s*;?\\s*\\?>", "/"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?site_url\\s*\\(\\s*\\)\\s*;?\\s*\\?>", "/"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:get_)?bloginfo\\s*\\(\\s*['\"]name['\"]\\s*\\)\\s*;?\\s*\\?>", "${web_name!''}"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:get_)?bloginfo\\s*\\(\\s*['\"]description['\"]\\s*\\)\\s*;?\\s*\\?>", "${seo_description!''}"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:get_)?bloginfo\\s*\\(\\s*['\"]url['\"]\\s*\\)\\s*;?\\s*\\?>", "/"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:get_)?bloginfo\\s*\\(\\s*['\"]charset['\"]\\s*\\)\\s*;?\\s*\\?>", "UTF-8"});
        GLOBAL_MAP.add(new String[]{php("date", "'Y'"), "${.now?string('yyyy')}"});
        GLOBAL_MAP.add(new String[]{php("date", "\"Y\""), "${.now?string('yyyy')}"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:comments_template|comment_form)\\s*\\([^)]*\\)\\s*;?\\s*\\?>",
                "<!-- TODO[评论]: FlyCms 评论体系与 WP 不同，请用 <@fly_article_comment_page> 等评论标签重写 -->"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?wp_nav_menu\\s*\\([^)]*\\)\\s*;?\\s*\\?>",
                "<!-- TODO[菜单]: 请用 <@fly_channel_tree fatherId=\"0\"> 渲染栏目导航 -->"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?wp_list_categories\\s*\\([^)]*\\)\\s*;?\\s*\\?>",
                "<!-- TODO[分类列表]: 请用 <@fly_category_model> 或 <@fly_channel_tree> 渲染 -->"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?dynamic_sidebar\\s*\\([^)]*\\)\\s*;?\\s*\\?>",
                "<!-- TODO[小部件]: FlyCms 用碎片替代，请在后台建碎片后用 <@fly_block key=\"你的碎片标识\"> 调用 -->"});
        GLOBAL_MAP.add(new String[]{
                "<\\?php\\s*(?:echo\\s+)?(?:the_tags|the_category|the_terms)\\s*\\([^)]*\\)\\s*;?\\s*\\?>",
                "<!-- TODO[分类/标签]: 请用 <@fly_fields_model> 取自定义字段，或后台建标签后调用 -->"});
    }

    /** WP 条件标签 → 说明（FlyCms 用模板层级区分页面类型，不需要条件判断） */
    private static final Map<String, String> CONDITIONAL_NOTE = new LinkedHashMap<>();
    static {
        CONDITIONAL_NOTE.put("is_home", "FlyCms 首页直接用 index.html，无需条件判断");
        CONDITIONAL_NOTE.put("is_front_page", "同上，用 index.html");
        CONDITIONAL_NOTE.put("is_single", "FlyCms 详情页用 detail.html，无需条件判断");
        CONDITIONAL_NOTE.put("is_page", "FlyCms 单页栏目用 page.html，无需条件判断");
        CONDITIONAL_NOTE.put("is_category", "FlyCms 分类列表用 list.html / list-{channel}.html");
        CONDITIONAL_NOTE.put("is_archive", "同上");
        CONDITIONAL_NOTE.put("is_search", "FlyCms 搜索页用 search.html");
        CONDITIONAL_NOTE.put("is_404", "FlyCms 错误页用 404.html");
        CONDITIONAL_NOTE.put("is_tag", "FlyCms 标签页用 tag.html");
    }

    /** The Loop 起始：<?php [if(have_posts()):] while(have_posts()): the_post(); ?> */
    private static final Pattern LOOP_OPEN = Pattern.compile(
            "<\\?php\\s*(?:if\\s*\\(\\s*have_posts\\s*\\(\\s*\\)\\s*\\)\\s*:\\s*)?"
                    + "while\\s*\\(\\s*have_posts\\s*\\(\\s*\\)\\s*\\)\\s*:\\s*the_post\\s*\\(\\s*\\)\\s*;?\\s*\\?>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOOP_END = Pattern.compile(
            "<\\?php\\s*endwhile", Pattern.CASE_INSENSITIVE);

    /** {@code <?php endif; ?>} */
    private static final Pattern ENDIF = Pattern.compile(
            "<\\?php\\s*endif\\s*;?\\s*\\?>", Pattern.CASE_INSENSITIVE);

    /** {@code <?php else : ?>}（独立标签写法） */
    private static final Pattern ELSE = Pattern.compile(
            "<\\?php\\s*else\\s*:?\\s*\\?>", Pattern.CASE_INSENSITIVE);

    /**
     * 成对条件块：{@code <?php if (XXX()) : ?>body<?php endif; ?>}。
     * 必须<b>成对</b>替换——只转 if 不转 endif 会留下孤立闭合标签，
     * 直接导致 FreeMarker 解析失败（比留 TODO 更糟）。
     */
    private static final List<String[]> IF_BLOCK_MAP = new ArrayList<>();
    static {
        IF_BLOCK_MAP.add(new String[]{"has_post_thumbnail", "image"});
        IF_BLOCK_MAP.add(new String[]{"has_excerpt", "description"});
        IF_BLOCK_MAP.add(new String[]{"has_tag", "keywords"});
    }

    @Autowired
    private ConfigService configService;

    // /////////////////// 主流程 ///////////////////

    /**
     * 上传 WordPress 主题包并转换为 FlyCms 皮肤。
     *
     * @param zip       主题 zip
     * @param skin      目标皮肤名（空则取包名）
     * @param overwrite 同名皮肤是否覆盖
     */
    public DataVo convert(MultipartFile zip, String skin, boolean overwrite) {
        if (zip == null || zip.isEmpty()) {
            return DataVo.failure("请选择 WordPress 主题包（zip）");
        }
        if (zip.getSize() > MAX_PACK_BYTES) {
            return DataVo.failure("主题包超过 50MB 上限");
        }
        File tmp = null;
        try {
            tmp = Files.createTempDirectory("flycms-wp-").toFile();
            File themeDir = unzip(zip, tmp);
            if (themeDir == null) {
                return DataVo.failure("主题包解析失败：未找到任何文件");
            }
            String targetSkin = StringUtils.isBlank(skin)
                    ? normalizeSkin(themeDir.getName()) : skin;
            if (StringUtils.isBlank(targetSkin) || !SKIN_NAME.matcher(targetSkin).matches()) {
                return DataVo.failure("皮肤名只允许字母/数字/下划线/中划线，1~50 位");
            }
            if (!overwrite && new File(TEMPLATE_ROOT, targetSkin).exists()) {
                return DataVo.failure("皮肤已存在：" + targetSkin + "，请勾选覆盖或改名");
            }
            return doConvert(themeDir, targetSkin);
        } catch (IOException e) {
            return DataVo.failure("主题包处理失败：" + e.getMessage());
        } finally {
            deleteDir(tmp);
        }
    }

    /**
     * 读取某皮肤的最近一次转换报告（TODO 清单）。
     *
     * <p>转换必然有损，报告是"还差什么"的唯一凭据；后台展示它，
     * 用户才知道要去改哪几处，而不是面对一个跑不通的模板发懵。
     */
    public DataVo report(String skin) {
        if (StringUtils.isBlank(skin) || !SKIN_NAME.matcher(skin).matches()) {
            return DataVo.failure("非法皮肤名");
        }
        File f = new File(new File(TEMPLATE_ROOT, skin), "CONVERT-REPORT.json");
        if (!f.isFile()) {
            return DataVo.failure("该皮肤没有转换报告，可能不是由 WordPress 主题转换而来");
        }
        try {
            String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            return DataVo.success("操作成功", JSON.parseObject(json));
        } catch (IOException e) {
            return DataVo.failure("报告读取失败：" + e.getMessage());
        }
    }

    private DataVo doConvert(File themeDir, String skin) {
        List<String> converted = new ArrayList<>();
        List<Map<String, String>> todo = new ArrayList<>();
        List<String> copied = new ArrayList<>();

        File tplRoot = new File(TEMPLATE_ROOT, skin);
        try {
            Files.createDirectories(tplRoot.toPath());
        } catch (IOException e) {
            return DataVo.failure("皮肤目录创建失败：" + e.getMessage());
        }

        // 1) 转换模板文件
        for (Map.Entry<String, String> e : FILE_MAP.entrySet()) {
            File src = new File(themeDir, e.getKey());
            if (!src.isFile()) {
                continue;
            }
            String flyFile = e.getValue();
            if (converted.contains(flyFile)) {
                todo.add(item(e.getKey(), "重复映射",
                        flyFile + " 已由前面的文件生成，已跳过（WP 多个源文件对应同一页面类型）"));
                continue;
            }
            convertFile(src, new File(tplRoot, flyFile), flyFile, skin, todo);
            converted.add(flyFile);
        }

        // 2) 复制静态资源到"资源目录"（不是模板目录）
        File assetRoot = new File(ASSET_ROOT, skin);
        for (String dir : RESOURCE_DIRS) {
            File src = new File(themeDir, dir);
            if (src.isDirectory()) {
                try {
                    copyDir(src.toPath(), assetRoot.toPath().resolve(dir));
                    copied.add(dir);
                } catch (IOException ignored) {
                    // 单个资源目录失败不阻断
                }
            }
        }
        File styleCss = new File(themeDir, "style.css");
        if (styleCss.isFile()) {
            try {
                Files.createDirectories(assetRoot.toPath().resolve("css"));
                Files.copy(styleCss.toPath(), assetRoot.toPath().resolve("css/style.css"),
                        StandardCopyOption.REPLACE_EXISTING);
                copied.add("css/style.css");
            } catch (IOException ignored) {
            }
        }
        File shot = new File(themeDir, "screenshot.png");
        if (shot.isFile()) {
            try {
                Files.copy(shot.toPath(), new File(tplRoot, "screenshot.png").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }

        // 3) 记录跳过文件
        for (Map.Entry<String, String> e : SKIP_FILES.entrySet()) {
            if (new File(themeDir, e.getKey()).isFile()) {
                todo.add(item(e.getKey(), "跳过不转换", e.getValue()));
            }
        }

        // 4) theme.json
        Map<String, Object> theme = buildThemeJson(themeDir, skin, converted);
        try {
            Files.write(new File(tplRoot, "theme.json").toPath(),
                    JSON.toJSONString(theme, true).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }

        // 5) 报告落盘
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("skin", skin);
        report.put("convertedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        report.put("convertedFiles", converted);
        report.put("copiedResources", copied);
        report.put("todoCount", todo.size());
        report.put("todo", todo);
        try {
            Files.write(new File(tplRoot, "CONVERT-REPORT.json").toPath(),
                    JSON.toJSONString(report, true).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", skin);
        data.put("theme", theme);
        data.put("convertedFiles", converted);
        data.put("copiedResources", copied);
        data.put("todoCount", todo.size());
        data.put("todo", todo);
        if (converted.isEmpty()) {
            return DataVo.failure("未找到可转换的模板文件，请确认上传的是 WordPress 主题包", data);
        }
        return DataVo.success("已转换为皮肤「" + skin + "」，模板 " + converted.size()
                + " 个，待人工处理 " + todo.size() + " 项", data);
    }

    // /////////////////// 模板转换 ///////////////////

    private void convertFile(File src, File dst, String flyFile, String skin,
                             List<Map<String, String>> todo) {
        String content;
        try {
            content = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }
        String type = PAGE_TYPE.getOrDefault(flyFile, "index");

        boolean loopDone = false;
        if ("list".equals(type) || "detail".equals(type)) {
            String[] r = convertLoop(content, flyFile);
            content = r[0];
            loopDone = "1".equals(r[1]);
        }
        if ("detail".equals(type) || (!loopDone && "list".equals(type))) {
            content = convertIfBlocks(content, "detail".equals(type) ? "info" : "item");
            content = applyMap(content, SINGLE_MAP);
        }
        content = applyGlobal(content, flyFile, skin, todo);
        content = stripResidualPhp(content, flyFile, todo);

        // 详情页若用了 info 变量，包一层 info_model
        if (content.contains("${(info.") || content.contains("${info.")) {
            if (!content.contains("<@fly_info_model")) {
                content = "<@fly_info_model model=\"${model.code}\" shortUrl=\"${shortUrl!}\">\n"
                        + content + "\n</@fly_info_model>";
            }
        }
        // 注入 skinPath
        if (content.contains("${skinPath}")) {
            content = "<#assign skinPath = \"/assets/skin/pc_theme/" + skin + "\">\n" + content;
        }
        try {
            Files.createDirectories(dst.toPath().getParent());
            Files.write(dst.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    /**
     * The Loop → {@code <@fly_page_model> + <#list>}。
     *
     * @return {内容, "1"表示转换成功}
     */
    private String[] convertLoop(String src, String file) {
        Matcher open = LOOP_OPEN.matcher(src);
        if (!open.find()) {
            return new String[]{src, "0"};
        }
        int start = open.end();
        Matcher close = LOOP_END.matcher(src);
        if (!close.find(start)) {
            return new String[]{src, "0"};
        }
        int end = close.start();
        String body = src.substring(start, end);
        body = body.replaceAll("(?i)<\\?php\\s*endwhile\\s*;?\\s*\\?>\\s*$", "");
        body = body.replaceAll("(?i)<\\?php\\s*endif\\s*;?\\s*\\?>\\s*$", "");
        body = convertIfBlocks(body, "item");
        body = applyMap(body, LOOP_MAP);

        String out = "<@fly_page_model model=\"${model.code}\" p=\"${p!1}\" rows=\"10\">\n"
                + "<#if dataList?? && dataList?size gt 0>\n"
                + "<#list dataList as item>\n"
                + body.trim() + "\n"
                + "</#list>\n"
                + "<#else>\n<p>暂无内容</p>\n</#if>\n"
                + "<#if pageHtml?? && pageHtml != ''>${pageHtml}</#if>\n"
                + "</@fly_page_model>";

        return new String[]{src.substring(0, open.start()) + out + src.substring(loopTail(src, end)), "1"};
    }

    /**
     * 计算 The Loop 结束位置：{@code endwhile} 之后还要吃掉 WP 的"空态分支"
     * （{@code else} … {@code endif}），因为我们已经用 {@code <#else>} 生成了"暂无内容"，
     * 否则会残留重复的空态文案和孤立的 {@code endif}。
     */
    private int loopTail(String src, int end) {
        int p = src.indexOf("?>", end);
        int after = (p != -1) ? p + 2 : end;
        String endwhileTag = src.substring(end, Math.min(end + 200, src.length())).toLowerCase();
        boolean elseInSameTag = endwhileTag.contains("else");
        Matcher endifM = ENDIF.matcher(src);
        if (!endifM.find(after)) {
            return after;
        }
        String mid = src.substring(after, endifM.start());
        if (elseInSameTag || ELSE.matcher(mid).find()) {
            return endifM.end();
        }
        return after;
    }

    /**
     * 成对条件块转换：{@code if (has_post_thumbnail()) : … endif;} → {@code <#if> … </#if>}。
     *
     * @param src 片段
     * @param var 数据变量名（{@code item} 列表 / {@code info} 详情）
     */
    private String convertIfBlocks(String src, String var) {
        for (String[] pair : IF_BLOCK_MAP) {
            Pattern p = Pattern.compile(
                    "<\\?php\\s*if\\s*\\(\\s*!?\\s*" + pair[0] + "\\s*\\(\\s*\\)\\s*\\)\\s*:\\s*\\?>"
                            + "(.*?)"
                            + "<\\?php\\s*endif\\s*;?\\s*\\?>",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            String repl = "<#if (" + var + "." + pair[1] + ")?? && " + var + "." + pair[1] + " != ''>"
                    + "$1" + "</#if>";
            src = p.matcher(src).replaceAll(Matcher.quoteReplacement(repl));
        }
        return src;
    }

    private String applyGlobal(String src, String file, String skin,
                               List<Map<String, String>> todo) {
        src = applyMap(src, GLOBAL_MAP);
        // 条件标签
        for (Map.Entry<String, String> e : CONDITIONAL_NOTE.entrySet()) {
            Pattern p = Pattern.compile(
                    "<\\?php\\s*if\\s*\\(\\s*" + e.getKey() + "\\s*\\(\\s*\\)\\s*\\)\\s*:\\s*\\?>",
                    Pattern.CASE_INSENSITIVE);
            if (p.matcher(src).find()) {
                todo.add(item(file, "条件标签", e.getKey() + "() —— " + e.getValue()));
                src = p.matcher(src).replaceAll("<!-- TODO: " + e.getValue() + " -->");
            }
        }
        return src;
    }

    /** 未识别的 PHP 块 → HTML 注释（保留现场，绝不静默丢弃） */
    private String stripResidualPhp(String src, String file, List<Map<String, String>> todo) {
        Matcher m = Pattern.compile("<\\?php.*?\\?>", Pattern.DOTALL).matcher(src);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String code = m.group();
            if (code.matches("<\\?php\\s*\\?>")) {
                m.appendReplacement(sb, "");
                continue;
            }
            todo.add(item(file, "未识别PHP块", clip(code, 200)));
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "<!-- TODO[WP_PHP]: " + clip(code.replace("-->", "--\\>"), 200) + " -->"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String applyMap(String src, List<String[]> map) {
        for (String[] pair : map) {
            src = Pattern.compile(pair[0], Pattern.CASE_INSENSITIVE)
                    .matcher(src)
                    .replaceAll(Matcher.quoteReplacement(pair[1]));
        }
        return src;
    }

    // /////////////////// theme.json ///////////////////

    private Map<String, Object> buildThemeJson(File themeDir, String skin, List<String> converted) {
        Map<String, String> meta = parseStyleHeader(new File(themeDir, "style.css"));
        Map<String, Object> tj = new LinkedHashMap<>();
        tj.put("name", defaultStr(meta.get("Theme Name"), skin));
        tj.put("code", skin);
        tj.put("version", defaultStr(meta.get("Version"), "1.0.0"));
        tj.put("author", defaultStr(meta.get("Author"), "converted-from-wordpress"));
        tj.put("parent", "");
        tj.put("thumbnail", new File(themeDir, "screenshot.png").isFile() ? "screenshot.png" : "");
        tj.put("description", defaultStr(meta.get("Description"), "由 WordPress 主题自动转换"));
        tj.put("engine", "freemarker");

        Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("source", "wordpress");
        origin.put("themeName", defaultStr(meta.get("Theme Name"), skin));
        origin.put("themeUri", defaultStr(meta.get("Theme URI"), ""));
        origin.put("convertedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        tj.put("origin", origin);

        List<String> parts = new ArrayList<>();
        for (String f : converted) {
            if (f.startsWith("parts/")) {
                parts.add(f.substring(6, f.length() - 5));
            }
        }
        Map<String, Object> supports = new LinkedHashMap<>();
        supports.put("templates", converted);
        supports.put("parts", parts);
        supports.put("device", Arrays.asList("pc"));
        tj.put("supports", supports);
        tj.put("customTemplates", new ArrayList<>());
        return tj;
    }

    /** 从 WP style.css 头部注释提取元信息 */
    private Map<String, String> parseStyleHeader(File styleCss) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (!styleCss.isFile()) {
            return meta;
        }
        String head;
        try {
            String all = new String(Files.readAllBytes(styleCss.toPath()), StandardCharsets.UTF_8);
            head = all.length() > 2000 ? all.substring(0, 2000) : all;
        } catch (IOException e) {
            return meta;
        }
        for (String key : Arrays.asList("Theme Name", "Theme URI", "Author", "Description", "Version")) {
            Matcher m = Pattern.compile("^\\s*\\*?\\s*" + Pattern.quote(key) + "\\s*:\\s*(.+)$",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(head);
            if (m.find()) {
                meta.put(key, m.group(1).trim());
            }
        }
        return meta;
    }

    // /////////////////// 解压（防 zip-slip） ///////////////////

    /**
     * 解压主题包到临时目录，返回主题根目录。
     * 若包内只有一个顶层目录（WP 主题包惯例），则进入该目录。
     */
    private File unzip(MultipartFile zip, File tmp) throws IOException {
        File root = new File(tmp, "unzipped");
        Files.createDirectories(root.toPath());
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String rel = stripName(entry.getName());
                if (rel.isEmpty()) {
                    continue;
                }
                String ext = extOf(rel);
                if (!UNZIP_EXT.contains(ext)) {
                    continue;
                }
                File dest = new File(root, rel);
                Path canonicalRoot = root.toPath().normalize().toAbsolutePath();
                Path resolved = dest.toPath().normalize().toAbsolutePath();
                if (!resolved.startsWith(canonicalRoot)) {
                    throw new IOException("主题包含非法路径条目，已中止：" + rel);
                }
                Files.createDirectories(resolved.getParent());
                Files.copy(zis, resolved, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        // 单一顶层目录则进入
        File[] children = root.listFiles();
        if (children != null && children.length == 1 && children[0].isDirectory()) {
            return children[0];
        }
        return root;
    }

    private String stripName(String name) {
        String n = name.replace('\\', '/');
        while (n.startsWith("./")) {
            n = n.substring(2);
        }
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        return n.contains("..") ? "" : n;
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i).toLowerCase();
    }

    private String normalizeSkin(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        return name.replaceAll("[^A-Za-z0-9_\\-]", "-").toLowerCase();
    }

    private String defaultStr(String v, String def) {
        return StringUtils.isBlank(v) ? def : v;
    }

    private String clip(String s, int max) {
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private Map<String, String> item(String file, String kind, String detail) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("file", file);
        m.put("kind", kind);
        m.put("detail", detail);
        return m;
    }

    // /////////////////// 文件工具 ///////////////////

    private void copyDir(Path src, Path target) throws IOException {
        File[] files = src.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            Path dest = target.resolve(f.getName());
            if (f.isDirectory()) {
                Files.createDirectories(dest);
                copyDir(f.toPath(), dest);
            } else {
                // 资源目录同样按白名单过滤：php 只能作为转换输入，绝不能落进皮肤
                if (!PACK_EXT.contains(extOf(f.getName()))) {
                    continue;
                }
                Files.createDirectories(dest.getParent());
                Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
