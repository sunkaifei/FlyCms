package com.flycms.module.template.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.channel.dao.ChannelDao;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.dao.ModelDao;
import com.flycms.module.template.dao.TemplateDao;
import com.flycms.module.template.model.TemplateFile;
import com.flycms.module.template.model.TemplateVersion;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 模板中心服务（规划 §8 阶段 D）
 *
 * 承接帝国"方案管理"、Dede"模板组"，并逐条规避 §6 的历史问题：
 * - §6.2 升级毁站：save 前写版本快照，任何一次保存都可回滚；
 * - §6.3 标签方言黑盒：保存前强制 Freemarker parse 校验，写坏不会导致全站 500；
 * - §9 风险 7 皮肤包引入恶意文件：import 逐 entry canonical 校验（zip-slip）+ 后缀白名单 + 50MB 上限；
 * - §9 风险 9 开缓存后"改了没生效"：保存后主动 removeTemplate 使当前进程即时生效。
 *
 * <b>文件系统为事实源</b>：磁盘上的皮肤目录始终是渲染依据，DB 只做登记与版本留痕，
 * 因此手工/FTP 改模板依然有效（运维后门保留）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TemplateCenterService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCenterService.class);

    /** 模板根：views/templates/pc_theme */
    public static final String THEME_ROOT = "views/templates/pc_theme";

    /** 在线编辑仅允许 .html（绝不产出可执行服务端脚本） */
    private static final java.util.regex.Pattern SAFE_TEMPLATE_PATH =
            java.util.regex.Pattern.compile("^[\\w\\-/\\u4e00-\\u9fa5]+\\.html$");

    /** 皮肤目录名白名单：禁止 ./ 与任意转义字符 */
    private static final java.util.regex.Pattern SKIN_NAME =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_\\-]{1,50}$");

    /** 皮肤包允许携带的资源后缀（白名单式，防带入 php/jsp 等可执行脚本） */
    private static final java.util.Set<String> PACK_EXT = new java.util.HashSet<>(
            java.util.Arrays.asList(".html", ".htm", ".css", ".js", ".json", ".md", ".txt",
                    ".jpg", ".jpeg", ".png", ".gif", ".svg", ".ico", ".webp", ".woff", ".woff2", ".ttf", ".eot"));

    /** 单文件大小上限 512KB（与在线编辑一致） */
    public static final long MAX_TEMPLATE_BYTES = 512 * 1024;
    /** 皮肤包总大小上限 50MB */
    public static final long MAX_PACK_BYTES = 50L * 1024 * 1024;
    /** 版本历史默认返回条数 */
    private static final int VERSION_ROWS = 100;

    @Autowired
    private TemplateDao templateDao;
    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ChannelDao channelDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Autowired(required = false)
    private ThemeRegistry themeRegistry;

    // /////////////////// 皮肤 ///////////////////

    /** 当前启用皮肤名（取自 fly_config_web.pc_theme） */
    public String currentSkin() {
        return StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
    }

    /** 全部皮肤目录 */
    public List<String> listSkins() {
        File root = new File(THEME_ROOT);
        List<String> skins = new ArrayList<>();
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File d : dirs) {
                skins.add(d.getName());
            }
        }
        return skins;
    }

    public File skinRoot(String skin) {
        return new File(THEME_ROOT + "/" + skin);
    }

    /**
     * D2 新建皮肤：从现有皮肤整体复制（换肤不影响在用皮肤）。
     *
     * @param skin  新皮肤名
     * @param from  模板来源皮肤，空则建空皮肤
     */
    public DataVo createSkin(String skin, String from) {
        if (StringUtils.isBlank(skin) || !SKIN_NAME.matcher(skin).matches()) {
            return DataVo.failure("皮肤名只允许字母/数字/下划线/中划线，1~50 位");
        }
        if (new File(THEME_ROOT, skin).exists()) {
            return DataVo.failure("皮肤目录已存在：" + skin);
        }
        File target = new File(THEME_ROOT, skin);
        if (!target.mkdirs()) {
            return DataVo.failure("皮肤目录创建失败");
        }
        int copied = 0;
        if (StringUtils.isNotBlank(from) && !from.equals(skin)) {
            File src = new File(THEME_ROOT, from);
            if (src.isDirectory()) {
                copied = copyDir(src.toPath(), target.toPath());
            }
        }
        return DataVo.success("皮肤已创建" + (copied > 0 ? "，已复制 " + copied + " 个文件" : ""));
    }

    /**
     * D3 删除皮肤：拒绝删当前使用皮肤，删除登记与版本记录。
     */
    public DataVo deleteSkin(String skin) {
        if (StringUtils.isBlank(skin) || !SKIN_NAME.matcher(skin).matches()) {
            return DataVo.failure("非法皮肤名");
        }
        if (skin.equals(currentSkin())) {
            return DataVo.failure("该皮肤正在使用，请先在网站管理切换到其它皮肤");
        }
        File dir = new File(THEME_ROOT, skin);
        if (!dir.isDirectory()) {
            return DataVo.failure("皮肤不存在：" + skin);
        }
        for (TemplateFile t : templateDao.findTemplatesBySkin(skin)) {
            templateDao.deleteVersions(t.getId());
        }
        templateDao.deleteTemplatesBySkin(skin);
        if (!deleteDir(dir.toPath())) {
            return DataVo.failure("皮肤目录删除失败，请检查文件占用");
        }
        return DataVo.success("皮肤已删除");
    }

    // /////////////////// 模板读/写 ///////////////////

    /**
     * 相对路径安全解析：正则白名单 + canonical 前缀校验。
     *
     * @return 目标文件；非法路径返回 null
     */
    public File resolve(String skin, String file) {
        if (StringUtils.isBlank(file) || file.contains("..")
                || !SAFE_TEMPLATE_PATH.matcher(file).matches()) {
            return null;
        }
        File root = skinRoot(skin);
        File f = new File(root, file);
        try {
            String canonicalRoot = root.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(canonicalRoot)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return f;
    }

    /**
     * D 保存链路的核心：语法校验 → 登记/取版本号 → 写快照 → 落盘 → 失效模板缓存。
     *
     * <b>顺序很重要</b>：先校验后落盘，保证"坏模板永不上线"；
     * 快照先于落盘写，保证任何一次上线动作都有对应的可回滚点。
     */
    public DataVo save(String skin, String file, String content, String remark, Long editorId) {
        File f = resolve(skin, file);
        if (f == null) {
            return DataVo.failure("非法模板路径");
        }
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEMPLATE_BYTES) {
            return DataVo.failure("模板内容超过 512KB 上限");
        }
        DataVo check = validateSyntax(file, content == null ? "" : content);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        TemplateFile tpl = templateDao.findTemplate(skin, file);
        if (tpl == null) {
            tpl = new TemplateFile();
            tpl.setId(SnowFlake.getInstance().nextId());
            tpl.setSkin(skin);
            tpl.setFilePath(file);
            tpl.setEditorId(editorId);
            templateDao.insertTemplate(tpl);
        } else {
            tpl.setEditorId(editorId);
            templateDao.updateTemplateTime(tpl);
        }
        int version = templateDao.maxVersion(tpl.getId()) + 1;
        TemplateVersion ver = new TemplateVersion();
        ver.setTemplateId(tpl.getId());
        ver.setVersion(version);
        ver.setContent(content);
        ver.setRemark(StringUtils.defaultString(remark));
        ver.setEditorId(editorId);
        templateDao.insertVersion(ver);
        try {
            Files.createDirectories(f.toPath().getParent());
            Files.write(f.toPath(), bytes);
        } catch (IOException e) {
            return DataVo.failure("保存失败：" + e.getMessage());
        }
        evictTemplate(skin, file);
        return DataVo.success("模板已保存，第 " + version + " 版");
    }

    /** 删除模板文件及其登记/版本历史（运营明确删的文件，不做文件级回滚保留） */
    public DataVo delete(String skin, String file) {
        File f = resolve(skin, file);
        if (f == null || !f.isFile()) {
            return DataVo.failure("模板文件不存在");
        }
        TemplateFile tpl = templateDao.findTemplate(skin, file);
        if (tpl != null) {
            templateDao.deleteVersions(tpl.getId());
            templateDao.deleteTemplate(skin, file);
        }
        if (!f.delete()) {
            return DataVo.failure("删除失败");
        }
        evictTemplate(skin, file);
        return DataVo.success("模板已删除");
    }

    /**
     * 语法校验（§6.3）：把"写完不知能不能跑"变成"写完就知道第几行错"。
     * 使用与线上同一份 Configuration，因此禁止项（沙箱 /api 阶段 A1）同步生效。
     */
    public DataVo validateSyntax(String name, String content) {
        try {
            Configuration cfg = freeMarkerConfigurer.getConfiguration();
            new Template(name, new StringReader(content), cfg).toString();
            return DataVo.success("ok");
        } catch (ParseException e) {
            return DataVo.failure("模板语法错误（第 " + e.getLineNumber() + " 行）：" + lineMessage(e));
        } catch (IOException e) {
            return DataVo.failure("模板读取失败：" + e.getMessage());
        }
    }

    // /////////////////// D1 版本历史与回滚 ///////////////////

    /** 版本历史（倒序），同时返回总数 */
    public Map<String, Object> versions(String skin, String file) {
        Map<String, Object> data = new LinkedHashMap<>();
        TemplateFile tpl = templateDao.findTemplate(skin, file);
        if (tpl == null) {
            data.put("versions", new ArrayList<>());
            data.put("total", 0);
            return data;
        }
        data.put("templateId", String.valueOf(tpl.getId()));
        data.put("versions", templateDao.findVersions(tpl.getId(), VERSION_ROWS));
        data.put("total", templateDao.countVersions(tpl.getId()));
        return data;
    }

    /**
     * 回滚：把历史版本内容作为<b>新版本</b>写入（历史永不被覆盖/删除，§6.2）。
     */
    public DataVo restore(String skin, String file, int version, Long editorId) {
        TemplateFile tpl = templateDao.findTemplate(skin, file);
        if (tpl == null) {
            return DataVo.failure("该模板尚无版本历史");
        }
        TemplateVersion ver = templateDao.findVersion(tpl.getId(), version);
        if (ver == null) {
            return DataVo.failure("版本不存在：" + version);
        }
        return save(skin, file, ver.getContent(), "回滚至第 " + version + " 版", editorId);
    }

    // /////////////////// D4/D5 皮肤包导出导入 ///////////////////

    /**
     * D4 导出皮肤包：标准 zip + manifest.json（皮肤名/版本/导出时间）。
     * 这是未来"模板市场"的分发格式（§5.2 劣势 1 的对策）。
     */
    public byte[] exportSkin(String skin) throws IOException {
        if (StringUtils.isBlank(skin) || !SKIN_NAME.matcher(skin).matches()) {
            throw new IOException("非法皮肤名");
        }
        File root = skinRoot(skin);
        if (!root.isDirectory()) {
            throw new IOException("皮肤不存在：" + skin);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            String manifest = "{\n  \"name\": \"" + skin + "\",\n  \"version\": \"1.0\",\n"
                    + "  \"exportTime\": \"" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
                    + "\",\n  \"engine\": \"freemarker\",\n  \"generator\": \"FlyCms Template Center\"\n}";
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(manifest.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            List<File> files = new ArrayList<>();
            collectFiles(root, files);
            for (File file : files) {
                String rel = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                String ext = extOf(file.getName());
                if (ext.isEmpty() || !PACK_EXT.contains(ext)) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(skin + "/" + rel));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    /**
     * D5 导入皮肤包：zip-slip 防御 + 后缀白名单 + 50MB 上限 + 重名拒收。
     *
     * @param overwrite true 时允许覆盖同名皮肤
     */
    public DataVo importSkin(MultipartFile zip, boolean overwrite) {
        if (zip == null || zip.isEmpty()) {
            return DataVo.failure("请选择皮肤包（zip）");
        }
        if (zip.getSize() > MAX_PACK_BYTES) {
            return DataVo.failure("皮肤包超过 50MB 上限");
        }
        String skin = null;
        List<String[]> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                String rel = stripName(name);
                if (rel.isEmpty()) {
                    continue;
                }
                if (skin == null) {
                    int i = rel.indexOf('/');
                    skin = i > 0 ? rel.substring(0, i) : null;
                }
                if (StringUtils.isBlank(skin) || !SKIN_NAME.matcher(skin).matches()) {
                    return DataVo.failure("皮肤包首层目录名非法：" + skin);
                }
                if (!rel.startsWith(skin + "/")) {
                    return DataVo.failure("皮肤包结构非法，所有文件必须位于皮肤目录内");
                }
                entries.add(new String[]{rel, readAll(zis)});
            }
        } catch (IOException e) {
            return DataVo.failure("皮肤包解析失败：" + e.getMessage());
        }
        if (skin == null) {
            return DataVo.failure("皮肤包为空");
        }
        if (skin.equals(currentSkin()) && !overwrite) {
            return DataVo.failure("皮肤名与正在使用的皮肤相同，请勾选覆盖或使用改名后的包");
        }
        boolean exist = new File(THEME_ROOT, skin).exists();
        if (exist && !overwrite) {
            return DataVo.failure("皮肤已存在：" + skin + "，请勾选覆盖导入");
        }
        int wrote = 0;
        int skipped = 0;
        for (String[] pair : entries) {
            String rel = pair[0];
            String target = rel.substring(skin.length() + 1);
            if (target.isEmpty()) {
                continue;
            }
            String ext = extOf(target);
            if (!PACK_EXT.contains(ext)) {
                skipped++;
                continue;
            }
            File dest = new File(new File(THEME_ROOT, skin), target);
            Path canonicalRoot = new File(THEME_ROOT, skin).toPath().normalize().toAbsolutePath();
            Path resolved = dest.toPath().normalize().toAbsolutePath();
            if (!resolved.startsWith(canonicalRoot)) {
                // zip-slip：条目越过皮肤根目录，直接拒绝整个包（不做部分导入）
                return DataVo.failure("皮肤包含非法路径条目，已中止导入：" + rel);
            }
            try {
                Files.createDirectories(resolved.getParent());
                Files.write(resolved, pair[1].getBytes(StandardCharsets.UTF_8));
                wrote++;
                if (".html".equals(ext)) {
                    evictTemplate(skin, target);
                }
            } catch (IOException e) {
                return DataVo.failure("写入失败：" + rel + " " + e.getMessage());
            }
        }
        return DataVo.success("皮肤「" + skin + "」已导入，写入 " + wrote + " 个文件"
                + (skipped > 0 ? "，跳过 " + skipped + " 个非白名单文件" : ""));
    }

    // /////////////////// D22 模板派生（另存为） ///////////////////

    /**
     * D22 模板派生：把已有模板<b>复制</b>成另一个层级槽位名，用户在此基础上改。
     *
     * <p>这是"上传 WP 主题 → 转成通用皮肤 → 派生出栏目/模型专属模板"链路的最后一环：
     * 皮肤里只有 {@code list.html} / {@code detail.html}，派生出 {@code list-news.html}
     * 后即可让新闻栏目单独换版式，其余栏目仍走 {@code list.html}——
     * 配合模板层级（§5），"换版式"从"复制整个目录"变成"新建 1 个文件"。
     *
     * <p>复用 {@link #save} 全链路：语法校验 → 版本快照 → 落盘 → 失效缓存，
     * 因此派生产物同样可回滚、改坏不会导致全站 500。
     *
     * @param skin      皮肤
     * @param file      源模板相对路径
     * @param target    目标模板名（须符合层级命名：{@code list-{channel}.html} / {@code detail-{model}.html} 等）
     * @param editorId  操作人
     * @param overwrite 目标已存在时是否覆盖
     */
    public DataVo derive(String skin, String file, String target, Long editorId, boolean overwrite) {
        File src = resolve(skin, file);
        if (src == null || !src.isFile()) {
            return DataVo.failure("源模板不存在：" + file);
        }
        File dst = resolve(skin, target);
        if (dst == null) {
            return DataVo.failure("目标文件名非法：只允许字母/数字/下划线/中划线/斜杠，且以 .html 结尾");
        }
        if (dst.exists() && !overwrite) {
            return DataVo.failure("目标模板已存在：" + target + "，请勾选覆盖");
        }
        String content = readContent(src);
        if (content == null) {
            return DataVo.failure("源模板读取失败：" + file);
        }
        return save(skin, target, content, "派生自 " + file, editorId);
    }

    /**
     * D22 派生槽位清单：按模板层级（§5）枚举"当前可以派生成哪些文件名"，
     * 并标出哪些已存在。后台 UI 用它渲染下拉，用户点一下就完成命名，不用背规则。
     *
     * <p>槽位来自两类真实数据：模型（{@code list-{code}} / {@code detail-{code}}）
     * 与栏目（{@code list-{dir}} / {@code detail-{dir}}），因此"有的选"而不是"自己猜"。
     *
     * @param skin 皮肤（用于判断文件是否已存在）
     */
    public Map<String, Object> deriveTargets(String skin) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();

        List<com.flycms.module.model.model.Model> models = new ArrayList<>();
        try {
            models = modelDao.getAllModelList(1);
        } catch (Exception ignored) {
            // 模型表不可用时降级为空列表，不影响模板派生本身
        }
        if (models != null) {
            for (com.flycms.module.model.model.Model m : models) {
                if (m == null || StringUtils.isBlank(m.getCode())) {
                    continue;
                }
                slots.add(slot(skin, "list-" + m.getCode() + ".html",
                        "模型列表", defaultStr2(m.getName(), m.getCode()), "model", m.getCode()));
                slots.add(slot(skin, "detail-" + m.getCode() + ".html",
                        "模型详情", defaultStr2(m.getName(), m.getCode()), "model", m.getCode()));
            }
        }

        List<com.flycms.module.channel.model.Channel> channels = new ArrayList<>();
        try {
            channels = channelDao.findAll();
        } catch (Exception ignored) {
        }
        if (channels != null) {
            for (com.flycms.module.channel.model.Channel c : channels) {
                if (c == null || StringUtils.isBlank(c.getChannelDir())) {
                    continue;
                }
                String dir = c.getChannelDir();
                slots.add(slot(skin, "list-" + dir + ".html",
                        "栏目列表", defaultStr2(c.getChannelName(), dir), "channel", dir));
                slots.add(slot(skin, "detail-" + dir + ".html",
                        "栏目详情", defaultStr2(c.getChannelName(), dir), "channel", dir));
            }
        }

        data.put("skin", skin);
        data.put("slots", slots);
        data.put("total", slots.size());
        return data;
    }

    private Map<String, Object> slot(String skin, String target, String kind,
                                     String label, String scope, String key) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("target", target);
        m.put("kind", kind);
        m.put("label", label + "（" + target + "）");
        m.put("scope", scope);
        m.put("key", key);
        File f = resolve(skin, target);
        m.put("exists", f != null && f.isFile());
        return m;
    }

    private String defaultStr2(String v, String def) {
        return StringUtils.isBlank(v) ? def : v;
    }

    // /////////////////// P4 子主题（声明式 parent） ///////////////////

    /**
     * P4 基于父主题创建子主题：只建目录 + 写 {@code theme.json}（声明 parent），<b>不复制任何文件</b>。
     * 子主题只放要改的文件，其余自动回退父主题（与 WordPress 同名覆盖一致，D18）。
     *
     * @param child  子主题目录名
     * @param parent 父主题目录名（必须存在）
     * @param name   子主题展示名（可空，默认=child）
     */
    public DataVo createChildSkin(String child, String parent, String name) {
        if (StringUtils.isBlank(child) || !SKIN_NAME.matcher(child).matches()) {
            return DataVo.failure("子主题名只允许字母/数字/下划线/中划线，1~50 位");
        }
        if (StringUtils.isBlank(parent) || !new File(THEME_ROOT, parent).isDirectory()) {
            return DataVo.failure("父主题不存在：" + parent);
        }
        if (new File(THEME_ROOT, child).exists()) {
            return DataVo.failure("子主题目录已存在：" + child);
        }
        File dir = new File(THEME_ROOT, child);
        if (!dir.mkdirs()) {
            return DataVo.failure("子主题目录创建失败");
        }
        writeThemeJson(dir, "{\n"
                + "  \"name\": \"" + (StringUtils.isBlank(name) ? child : name) + "\",\n"
                + "  \"code\": \"" + child + "\",\n"
                + "  \"version\": \"1.0.0\",\n"
                + "  \"author\": \"FlyCms\",\n"
                + "  \"parent\": \"" + parent + "\",\n"
                + "  \"engine\": \"freemarker\",\n"
                + "  \"description\": \"基于 " + parent + " 的子主题\"\n"
                + "}");
        if (themeRegistry != null) {
            themeRegistry.refresh();
        }
        return DataVo.success("子主题已创建：" + child + "（父主题 " + parent + "，现在只放要改的文件即可）");
    }

    /**
     * P4 把父主题的若干文件复制到子主题（对应 WP 手工复制）。仍提示"只复制要改的"——
     * 每个多复制的文件都会失去父主题后续的安全修复。
     *
     * @param child  子主题
     * @param parent 父主题
     * @param files  相对文件路径列表（如 parts/header.html、list.html）
     */
    public DataVo copyParentFiles(String child, String parent, List<String> files) {
        if (StringUtils.isBlank(child) || StringUtils.isBlank(parent)) {
            return DataVo.failure("子主题/父主题名不能为空");
        }
        File childDir = new File(THEME_ROOT, child);
        File parentDir = new File(THEME_ROOT, parent);
        if (!parentDir.isDirectory()) {
            return DataVo.failure("父主题不存在：" + parent);
        }
        if (!childDir.isDirectory()) {
            return DataVo.failure("子主题不存在：" + child + "，请先创建子主题");
        }
        if (files == null || files.isEmpty()) {
            return DataVo.failure("请选择要复制的文件");
        }
        int n = 0;
        for (String rel : files) {
            if (StringUtils.isBlank(rel) || rel.contains("..") || !SAFE_TEMPLATE_PATH.matcher(rel).matches()) {
                continue;
            }
            File src = new File(parentDir, rel);
            if (!src.isFile()) {
                continue;
            }
            try {
                File dst = new File(childDir, rel);
                Files.createDirectories(dst.getParentFile().toPath());
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                evictTemplate(child, rel);
                n++;
            } catch (IOException ignored) {
            }
        }
        if (themeRegistry != null) {
            themeRegistry.refresh();
        }
        return DataVo.success("已从父主题复制 " + n + " 个文件到子主题 " + child);
    }

    /** 写 theme.json（UTF-8） */
    private void writeThemeJson(File dir, String json) {
        try {
            Files.write(new File(dir, "theme.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warn("theme.json 写入失败：{}", e.getMessage());
        }
    }

    // /////////////////// D8 试渲染 ///////////////////

    /**
     * D8 模板试渲染：不落盘，直接用当前编辑内容渲染一遍，用于"写完立刻知道效果"。
     * 渲染失败返回具体错误而不是抛异常到前台。
     */
    public DataVo preview(String content, Map<String, Object> model) {
        DataVo check = validateSyntax("__preview__", content == null ? "" : content);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        try {
            Configuration cfg = freeMarkerConfigurer.getConfiguration();
            Template tpl = new Template("__preview__", new StringReader(content), cfg);
            java.io.StringWriter out = new java.io.StringWriter();
            tpl.process(model == null ? new java.util.HashMap<>() : model, out);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("html", out.toString());
            return DataVo.success("操作成功", data);
        } catch (Exception e) {
            return DataVo.failure("试渲染失败：" + firstLine(e.getMessage()));
        }
    }

    /** 试渲染的样例数据：给标签提供最小可用变量，避免所有值都为空看不出排版 */
    public Map<String, Object> sampleModel(String modelCode) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("siteName", StringUtils.defaultIfBlank(configService.getStringByKey("fly_title"), "示例站点"));
        m.put("p", 1);
        if (StringUtils.isBlank(modelCode)) {
            return m;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", "1");
        info.put("title", "这是一条示例内容");
        info.put("shortUrl", "demo");
        info.put("content", "<p>用于预览模板渲染效果的示例正文。</p>");
        info.put("keywords", "示例,预览");
        info.put("description", "示例内容的描述文字");
        info.put("createTime", "2026-01-01 12:00");
        info.put("countView", 128);
        m.put("info", info);
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", String.valueOf(i));
            row.put("title", "示例内容标题 " + i);
            row.put("shortUrl", "demo" + i);
            row.put("createTime", "2026-01-0" + i + " 12:00");
            list.add(row);
        }
        m.put("dataList", list);
        m.put("modelCode", modelCode);
        return m;
    }

    // /////////////////// 内部 ///////////////////

    /** 保存/删除/导入后主动失效模板缓存：防止"改了没生效"（§9 风险 9） */
    public void evictTemplate(String skin, String file) {
        try {
            // FreeMarker 2.3.x 的失效 API 是 removeTemplateFromCache（Configuration 上无 removeTemplate）
            freeMarkerConfigurer.getConfiguration()
                    .removeTemplateFromCache("pc_theme/" + skin + "/" + file);
        } catch (Exception ignored) {
            // 模板从未被加载时移除会抛异常，忽略
        }
    }

    /**
     * P9 一键清空模板缓存（§10）：FreeMarker 全量模板缓存 + 主题注册表元信息缓存。
     * 用于"改了没生效"的最后手段——清完下次访问按磁盘当前内容重新加载。
     */
    public void clearAllTemplateCache() {
        try {
            freeMarkerConfigurer.getConfiguration().clearTemplateCache();
        } catch (Exception ignored) {
            // 缓存为空时部分实现会抛异常，忽略
        }
        if (themeRegistry != null) {
            themeRegistry.refresh();
        }
    }

    /** 遍历收集全部文件（用于导出与打包） */
    public void collectFiles(File root, List<File> out) {
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectFiles(f, out);
            } else {
                out.add(f);
            }
        }
    }

    private String stripName(String name) {
        String n = name.replace('\\', '/');
        while (n.startsWith("./")) {
            n = n.substring(2);
        }
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        if (n.contains("..")) {
            return "";
        }
        return n;
    }

    private String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i).toLowerCase(java.util.Locale.ROOT);
    }

    private int copyDir(Path src, Path target) {
        int n = 0;
        File[] files = src.toFile().listFiles();
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            try {
                Path dest = Paths.get(target.toString(), f.getName());
                if (f.isDirectory()) {
                    Files.createDirectories(dest);
                    n += copyDir(f.toPath(), dest);
                } else {
                    Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    n++;
                }
            } catch (IOException ignored) {
            }
        }
        return n;
    }

    private boolean deleteDir(Path dir) {
        File[] files = dir.toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (!deleteDir(f.toPath())) {
                        return false;
                    }
                } else if (!f.delete()) {
                    return false;
                }
            }
        }
        return dir.toFile().delete();
    }

    private String lineMessage(ParseException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "未知错误";
        }
        return msg.replaceAll("\\s+", " ").trim();
    }

    private String firstLine(String msg) {
        if (msg == null) {
            return "未知错误";
        }
        String s = msg.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    /** 读取文件内容（UTF-8）；不存在返回 null */
    public String readContent(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** 单文件 white-list 校验后读取流（导出时兜底），非法返回 null */
    public InputStream open(File f) throws IOException {
        return new FileInputStream(f);
    }
}
