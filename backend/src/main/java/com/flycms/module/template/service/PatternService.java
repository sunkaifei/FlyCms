package com.flycms.module.template.service;

import com.flycms.core.entity.DataVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 区块图案服务（规划 §7.1 第三类可复用单元 / §8.2 组件面板）。
 *
 * <p><b>与"模板部件"的区别</b>（§7.1 表格）：
 * <ul>
 *   <li>模板部件 {@code parts/*.html} —— 页头页脚这类"每个页面都要有"的位置件，被 include 引用；</li>
 *   <li>区块图案 {@code patterns/*.html} —— 预设的图文组合 / CTA 区这类"想加一块内容时抄一段"的素材，
 *       <b>只读展示 + 一键插入</b>，不参与解析链、不被自动引用。</li>
 * </ul>
 * 因此本服务只有"列 + 读"，没有写接口：图案是主题作者放进包里的素材，
 * 要改就在模板编辑器里改，避免同一份内容有两个可写入口。
 *
 * <p><b>元信息约定</b>：图案文件第一行可写
 * {@code <#-- name: 图文两栏 CTA | desc: 左侧文案右侧按钮 -->}，
 * 供后台列表展示；不写则用文件名兜底。解析失败不影响读取。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class PatternService {

    private static final Logger logger = LoggerFactory.getLogger(PatternService.class);

    /** 图案目录名（皮肤根下） */
    public static final String PATTERN_DIR = "patterns";

    /** 单文件大小上限（与在线编辑一致，避免把整站 HTML 塞进图案） */
    private static final long MAX_BYTES = 512 * 1024;

    /** 文件名白名单：字母数字下划线中划线 + .html */
    private static final Pattern SAFE_NAME =
            Pattern.compile("^[A-Za-z0-9_\\-]{1,64}\\.html$");

    /** 皮肤名白名单 */
    private static final Pattern SAFE_SKIN =
            Pattern.compile("^[A-Za-z0-9_\\-]{1,50}$");

    /** 首行注释元信息：name: xxx | desc: yyy */
    private static final Pattern META_LINE =
            Pattern.compile("^\\s*<#--\\s*(.*?)\\s*-->\\s*$");

    /** 列出某皮肤下的全部图案（只读展示用） */
    public List<Map<String, Object>> list(String skin) {
        List<Map<String, Object>> out = new ArrayList<>();
        File dir = patternDir(skin);
        if (dir == null) {
            return out;
        }
        File[] files = dir.listFiles((d, name) -> SAFE_NAME.matcher(name).matches());
        if (files == null) {
            return out;
        }
        Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        for (File f : files) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("file", f.getName());
            m.put("size", f.length());
            Map<String, String> meta = readMeta(f);
            m.put("name", StringUtils.defaultIfBlank(meta.get("name"),
                    f.getName().replaceAll("\\.html$", "")));
            m.put("desc", StringUtils.defaultString(meta.get("desc")));
            out.add(m);
        }
        return out;
    }

    /** 读取单个图案内容（供编辑器"插入"） */
    public DataVo read(String skin, String file) {
        if (StringUtils.isBlank(file) || !SAFE_NAME.matcher(file).matches()) {
            return DataVo.failure("非法的图案文件名");
        }
        File dir = patternDir(skin);
        if (dir == null) {
            return DataVo.failure("皮肤名非法或皮肤不存在");
        }
        File f = new File(dir, file);
        // canonical 前缀校验：防 ../ 逃逸（文件名白名单已排除，这里是第二道）
        try {
            if (!f.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
                return DataVo.failure("非法路径");
            }
        } catch (Exception e) {
            return DataVo.failure("路径校验失败");
        }
        if (!f.isFile()) {
            return DataVo.failure("图案不存在：" + file);
        }
        if (f.length() > MAX_BYTES) {
            return DataVo.failure("图案文件超过 " + (MAX_BYTES / 1024) + "KB，无法在线插入");
        }
        try {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("file", file);
            data.put("skin", skin);
            data.put("content", content);
            Map<String, String> meta = readMeta(f);
            data.put("name", StringUtils.defaultIfBlank(meta.get("name"),
                    file.replaceAll("\\.html$", "")));
            data.put("desc", StringUtils.defaultString(meta.get("desc")));
            return DataVo.success("操作成功", data);
        } catch (Exception e) {
            logger.warn("读取图案失败：{}，原因={}", file, e.getMessage());
            return DataVo.failure("读取失败：" + e.getMessage());
        }
    }

    /** 该皮肤是否存在 patterns 目录（UI 决定是否显示"图案库"入口） */
    /**
     * V5 模板片段库：保存自定义图案（同名覆盖）。name 白名单同 list（小写字母数字_-），
     * 建议带 custom- 前缀以区分内置示例；content 保存前做 FreeMarker 语法校验由调用方负责。
     */
    public DataVo save(String skin, String name, String content) {
        File dir = patternDir(skin);
        if (dir == null) {
            return DataVo.failure("主题不存在或无 patterns 目录");
        }
        String file = withHtmlSuffix(name);
        if (!SAFE_NAME.matcher(file).matches()) {
            return DataVo.failure("图案名仅允许字母/数字/_-（1~64 位，.html 结尾）");
        }
        try {
            java.nio.file.Files.write(new File(dir, file).toPath(),
                    (content == null ? "" : content).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return DataVo.success("图案已保存：" + file);
        } catch (Exception e) {
            return DataVo.failure("保存失败：" + e.getMessage());
        }
    }

    /** V5：删除自定义图案（内置示例同理可删，git 可回滚） */
    public DataVo delete(String skin, String name) {
        File dir = patternDir(skin);
        if (dir == null) {
            return DataVo.failure("主题不存在或无 patterns 目录");
        }
        String file = withHtmlSuffix(name);
        if (!SAFE_NAME.matcher(file).matches()) {
            return DataVo.failure("图案名不合法");
        }
        File f = new File(dir, file);
        try {
            if (!f.exists() || !f.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
                return DataVo.failure("图案不存在");
            }
            return f.delete() ? DataVo.success("已删除") : DataVo.failure("删除失败");
        } catch (java.io.IOException e) {
            return DataVo.failure("删除失败：" + e.getMessage());
        }
    }

    /** 与 read 同口径：name 自动补 .html 后缀 */
    private static String withHtmlSuffix(String file) {
        return file != null && file.regionMatches(true, file.length() - 5, ".html", 0, 5)
                ? file : file + ".html";
    }

    public boolean hasPatterns(String skin) {
        File dir = patternDir(skin);
        return dir != null && dir.isDirectory() && !list(skin).isEmpty();
    }

    private File patternDir(String skin) {
        String s = StringUtils.trimToEmpty(skin);
        if (StringUtils.isBlank(s) || !SAFE_SKIN.matcher(s).matches()) {
            return null;
        }
        File dir = new File(new File(TemplateCenterService.THEME_ROOT, s), PATTERN_DIR);
        return dir.isDirectory() ? dir : null;
    }

    /** 解析首行 {@code <#-- name: x | desc: y -->} 元信息 */
    private Map<String, String> readMeta(File f) {
        Map<String, String> meta = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                Matcher m = META_LINE.matcher(lines.get(i));
                if (!m.matches()) {
                    continue;
                }
                for (String part : m.group(1).split("\\|")) {
                    int c = part.indexOf(':');
                    if (c <= 0) {
                        continue;
                    }
                    String k = part.substring(0, c).trim().toLowerCase();
                    String v = part.substring(c + 1).trim();
                    if ("name".equals(k) || "desc".equals(k)) {
                        meta.put(k, v);
                    }
                }
                break;
            }
        } catch (Exception e) {
            logger.debug("解析图案元信息失败：{}，原因={}", f.getName(), e.getMessage());
        }
        return meta;
    }
}
