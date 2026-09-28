package com.flycms.module.template.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.template.dao.ThemeDao;
import com.flycms.module.template.model.Theme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主题注册表（规划 §6.1 / D21 / P1）。
 *
 * <p>职责：扫描皮肤目录、解析 {@code theme.json}、维护父子继承链、提供主题元信息与能力清单。
 * <b>文件系统为事实源</b>：解析模板时定位文件走磁盘（含子主题→父主题回退），
 * 本类的元信息缓存仅用于"主题名/父主题/能力声明"，不缓存文件存在性——
 * 因此新建模板文件会立即被解析器发现。
 *
 * <p>无 {@code theme.json} 的旧皮肤自动生成默认清单（name=目录名、parent 空、
 * templates=实际扫描到的 html），存量皮肤零改动兼容（D21）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ThemeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ThemeRegistry.class);

    /** 模板根：views/templates/pc_theme */
    public static final String THEME_ROOT = "views/templates/pc_theme";

    /** 元信息缓存 TTL（毫秒），过期后下次访问重新扫描；主题增删/theme.json 变更时手动 {@link #refresh()} */
    private static final long TTL = 60_000L;

    @Autowired
    private ConfigService configService;
    @Autowired(required = false)
    private ThemeDao themeDao;

    /** code -> Theme 元信息缓存 */
    private final Map<String, Theme> cache = new ConcurrentHashMap<>();
    private volatile long cacheTime = 0L;
    private final Object scanLock = new Object();

    /** 当前启用皮肤（取自 fly_config_web.pc_theme） */
    public String currentSkin() {
        return StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
    }

    /** 使元信息缓存失效（主题增删/theme.json 变更/切换后调用） */
    public void refresh() {
        synchronized (scanLock) {
            cache.clear();
            cacheTime = 0L;
        }
    }

    /** 全部主题（含元信息），标记 isCurrent */
    public List<Theme> allThemes() {
        scan();
        List<Theme> list = new ArrayList<>(cache.values());
        String cur = currentSkin();
        for (Theme t : list) {
            t.setIsCurrent(t.getCode() != null && t.getCode().equals(cur) ? 1 : 0);
        }
        list.sort((a, b) -> StringUtils.compare(a.getCode(), b.getCode()));
        return list;
    }

    /** 单个主题元信息（无则 null） */
    public Theme getTheme(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        scan();
        return cache.get(code);
    }

    /** 子主题→父主题定位文件：返回命中主题与视图名（视图名不含 .html 后缀，由 FreeMarker 视图解析器追加） */
    public Resolved locate(String skin, String relativeFile) {
        if (StringUtils.isBlank(skin) || StringUtils.isBlank(relativeFile)) {
            return null;
        }
        // 视图名统一不含 .html（与 TemplateService.getPcTemplate 约定一致，FreeMarker 解析器会补 .html）
        String base = relativeFile.endsWith(".html")
                ? relativeFile.substring(0, relativeFile.length() - 5) : relativeFile;
        scan();
        String cur = skin;
        while (cur != null) {
            File f = new File(THEME_ROOT, cur + "/" + base + ".html");
            if (f.exists() && f.isFile()) {
                return new Resolved(cur, "pc_theme/" + cur + "/" + base);
            }
            Theme t = cache.get(cur);
            cur = (t != null && StringUtils.isNotBlank(t.getParentCode())) ? t.getParentCode() : null;
        }
        return null;
    }

    /** locate 的命中结果 */
    public static class Resolved {
        public final String skin;
        public final String view;
        public Resolved(String skin, String view) {
            this.skin = skin;
            this.view = view;
        }
    }

    /**
     * 切换兼容性预检（§6.4 / P5）。
     * <ul>
     *   <li>缺失 {@code index.html}：BLOCK（致命，切过去必白屏）</li>
     *   <li>缺失 {@code list.html} / {@code detail.html}：WARN（部分页面回退 index，仍可运行）</li>
     *   <li>父主题声明但不存在：BLOCK</li>
     * </ul>
     */
    public CompatResult checkCompatibility(String code) {
        CompatResult r = new CompatResult();
        if (StringUtils.isBlank(code) || getTheme(code) == null) {
            r.getBlocks().add("主题不存在：" + code);
            return r;
        }
        Theme t = getTheme(code);
        if (locate(code, "index.html") == null) {
            r.getBlocks().add("缺少兜底模板 index.html（切过去任意页面都可能白屏）");
        }
        if (locate(code, "list.html") == null) {
            r.getWarnings().add("缺少通用列表模板 list.html（列表页将回退到 index.html）");
        }
        if (locate(code, "detail.html") == null) {
            r.getWarnings().add("缺少通用详情模板 detail.html（详情页将回退到 index.html）");
        }
        if (StringUtils.isNotBlank(t.getParentCode()) && getTheme(t.getParentCode()) == null) {
            r.getBlocks().add("声明的父主题不存在：" + t.getParentCode());
        }
        return r;
    }

    /** 兼容性预检结果 */
    public static class CompatResult {
        private final List<String> blocks = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        public List<String> getBlocks() {
            return blocks;
        }
        public List<String> getWarnings() {
            return warnings;
        }
        public boolean isOk() {
            return blocks.isEmpty();
        }
    }

    // /////////////////// 内部扫描 ///////////////////

    private void scan() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && (now - cacheTime) < TTL) {
            return;
        }
        synchronized (scanLock) {
            if (!cache.isEmpty() && (System.currentTimeMillis() - cacheTime) < TTL) {
                return;
            }
            cache.clear();
            File root = new File(THEME_ROOT);
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File d : dirs) {
                    // 跳过下划线/点开头的目录：约定为草稿、测试数据（如 _testdata_draft）与 VCS 目录，
                    // 不登记为可切换主题，避免自测产物混进主题市场（§8.1）
                    if (d.getName().startsWith("_") || d.getName().startsWith(".")) {
                        continue;
                    }
                    Theme t = parseTheme(d);
                    if (t != null) {
                        cache.put(t.getCode(), t);
                    }
                }
            }
            cacheTime = System.currentTimeMillis();
            // 后台登记到 fly_theme（best-effort，不阻塞渲染）
            if (themeDao != null) {
                for (Theme t : cache.values()) {
                    try {
                        t.setCreateTime(new Date());
                        themeDao.upsert(t);
                    } catch (Exception e) {
                        logger.debug("theme upsert skip: {} - {}", t.getCode(), e.getMessage());
                    }
                }
            }
        }
    }

    /** 解析单个皮肤目录的 theme.json；缺失则生成默认清单 */
    private Theme parseTheme(File dir) {
        String code = dir.getName();
        File json = new File(dir, "theme.json");
        Theme theme = new Theme();
        theme.setCode(code);
        theme.setDevice("pc");
        theme.setStatus(1);
        if (!json.exists()) {
            return defaultTheme(code, dir);
        }
        try {
            String text = new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
            JSONObject o = JSONObject.parseObject(text);
            if (o == null) {
                return defaultTheme(code, dir);
            }
            theme.setName(StringUtils.defaultIfBlank(o.getString("name"), code));
            theme.setVersion(o.getString("version"));
            theme.setAuthor(o.getString("author"));
            theme.setParentCode(o.getString("parent"));
            theme.setThumbnail(o.getString("thumbnail"));
            theme.setDescription(o.getString("description"));
            if (StringUtils.isNotBlank(o.getString("device"))) {
                theme.setDevice(o.getString("device"));
            }
            theme.setConfigJson(text);
            JSONObject supports = o.getJSONObject("supports");
            if (supports != null) {
                JSONArray tmpl = supports.getJSONArray("templates");
                if (tmpl != null) {
                    for (int i = 0; i < tmpl.size(); i++) {
                        theme.getSupportsTemplates().add(tmpl.getString(i));
                    }
                }
                JSONArray regs = supports.getJSONArray("regions");
                if (regs != null) {
                    for (int i = 0; i < regs.size(); i++) {
                        theme.getRegions().add(regs.getString(i));
                    }
                }
            }
            JSONArray custom = o.getJSONArray("customTemplates");
            if (custom != null) {
                for (int i = 0; i < custom.size(); i++) {
                    JSONObject c = custom.getJSONObject(i);
                    if (c == null) {
                        continue;
                    }
                    Theme.CustomTemplate ct = new Theme.CustomTemplate();
                    ct.setFile(c.getString("file"));
                    ct.setName(c.getString("name"));
                    JSONArray pts = c.getJSONArray("postTypes");
                    if (pts != null) {
                        for (int j = 0; j < pts.size(); j++) {
                            ct.getPostTypes().add(pts.getString(j));
                        }
                    }
                    theme.getCustomTemplates().add(ct);
                }
            }
            JSONObject settings = o.getJSONObject("settings");
            if (settings != null) {
                parseSettings(theme.getSettings(), settings);
            }
            return theme;
        } catch (Exception e) {
            logger.warn("主题 {} 的 theme.json 解析失败，改用默认清单：{}", code, e.getMessage());
            return defaultTheme(code, dir);
        }
    }

    private void parseSettings(Theme.ThemeSettings target, JSONObject settings) {
        JSONObject color = settings.getJSONObject("color");
        if (color != null) {
            JSONArray palette = color.getJSONArray("palette");
            if (palette != null) {
                for (int i = 0; i < palette.size(); i++) {
                    JSONObject p = palette.getJSONObject(i);
                    if (p == null) {
                        continue;
                    }
                    Theme.PaletteColor pc = new Theme.PaletteColor();
                    pc.setSlug(p.getString("slug"));
                    pc.setColor(p.getString("color"));
                    pc.setName(p.getString("name"));
                    target.getPalette().add(pc);
                }
            }
        }
        JSONObject typo = settings.getJSONObject("typography");
        if (typo != null) {
            JSONArray sizes = typo.getJSONArray("fontSizes");
            if (sizes != null) {
                for (int i = 0; i < sizes.size(); i++) {
                    JSONObject s = sizes.getJSONObject(i);
                    if (s == null) {
                        continue;
                    }
                    Theme.FontSize fs = new Theme.FontSize();
                    fs.setSlug(s.getString("slug"));
                    fs.setSize(s.getString("size"));
                    target.getFontSizes().add(fs);
                }
            }
        }
        JSONObject layout = settings.getJSONObject("layout");
        if (layout != null) {
            target.setContentSize(layout.getString("contentSize"));
            target.setWideSize(layout.getString("wideSize"));
        }
    }

    /** 无 theme.json 时生成默认清单：name=目录名、parent 空、templates=实际扫描到的 html 名 */
    private Theme defaultTheme(String code, File dir) {
        Theme theme = new Theme();
        theme.setCode(code);
        theme.setName(code);
        theme.setDevice("pc");
        theme.setStatus(1);
        theme.setDescription("（未提供 theme.json，已自动生成默认清单）");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files != null) {
            for (File f : files) {
                String n = f.getName().replaceAll("\\.html$", "");
                theme.getSupportsTemplates().add(n);
            }
        }
        return theme;
    }
}
