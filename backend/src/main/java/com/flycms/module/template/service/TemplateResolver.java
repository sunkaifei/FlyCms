package com.flycms.module.template.service;

import com.flycms.module.template.dao.TemplateAssignDao;
import com.flycms.module.template.model.TemplateAssign;
import com.flycms.module.template.model.TemplateContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一模板解析入口（规划 §4.2 / D15 / P2）。<b>整个系统只有这一个地方决定"用哪个模板"</b>，
 * 三处 {@code resolveTemplate}（ModelController / ChannelRenderService / TemplateService）全部收敛到此。
 *
 * <p>规则：最具体优先、命中即停、{@code index.html} 兜底；第一优先级恒为 DB 指派
 * （{@code fly_template_assign}，对应 WP 后台 Page Template 下拉）。
 *
 * <p>定位走 {@link ThemeRegistry#locate}，自动处理子主题→父主题回退。解析结果回填到
 * {@link TemplateContext}，供前台调试条与后台"候选链可视化"使用。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TemplateResolver {

    private static final Logger logger = LoggerFactory.getLogger(TemplateResolver.class);

    @Autowired
    private ThemeRegistry registry;
    @Autowired(required = false)
    private TemplateAssignDao assignDao;
    @Autowired
    private com.flycms.module.config.service.ConfigService configService;

    /** 候选链节点（供调试条/后台 UI） */
    public static class Candidate {
        /** 相对文件，如 list-news.html（无后缀） */
        public final String file;
        /** 该候选的来源说明（DB指派 / 层级 / 兼容） */
        public final String source;
        /** 命中后的视图名（未命中 null） */
        public String view;
        /** 命中的真实主题（子主题覆盖时为父主题） */
        public String theme;
        /** 是否存在 */
        public boolean exists;

        public Candidate(String file, String source) {
            this.file = file;
            this.source = source;
        }
    }

    // /////////////////// 对外便捷方法（供现有 Controller 收敛用） ///////////////////

    public String resolveModelList(String modelCode, String channelDir) {
        return resolve(TemplateContext.list(modelCode, channelDir));
    }

    public String resolveModelDetail(String modelCode, String shortUrl, Long contentId, String channelDir) {
        return resolve(TemplateContext.detail(modelCode, shortUrl, contentId, channelDir));
    }

    public String resolveChannelList(String channelDir, String modelCode) {
        return resolve(TemplateContext.list(modelCode, channelDir));
    }

    public String resolveChannelPage(String channelDir) {
        return resolve(TemplateContext.channelPage(channelDir));
    }

    public String resolveIndex() {
        return resolve(TemplateContext.index());
    }

    public String resolveError(int code) {
        return resolve(TemplateContext.error(code));
    }

    public String resolveSearch() {
        return resolve(TemplateContext.search());
    }

    public String resolveTag(String tag) {
        return resolve(TemplateContext.tag(tag));
    }

    // /////////////////// 核心 ///////////////////

    /**
     * 解析模板：生成候选链 → 依次 locate → 命中即返回视图名 → 全不命中回退 index → 再不命中回 404。
     */
    public String resolve(TemplateContext ctx) {
        String skin = activeSkin();
        List<Candidate> chain = buildChain(ctx);

        // 1) DB 指派最高优先级
        String assign = assignedTemplate(ctx);
        if (StringUtils.isNotBlank(assign)) {
            ThemeRegistry.Resolved r = registry.locate(skin, assign);
            if (r != null) {
                fill(ctx, r, assign, "DB指派");
                return r.view;
            }
        }

        // 2) 层级候选链
        for (Candidate c : chain) {
            ThemeRegistry.Resolved r = registry.locate(skin, c.file);
            if (r != null) {
                c.exists = true;
                c.view = r.view;
                c.theme = r.skin;
                fill(ctx, r, c.file, c.source);
                return r.view;
            }
        }

        // 3) 兜底 index
        ThemeRegistry.Resolved idx = registry.locate(skin, "index");
        if (idx != null) {
            fill(ctx, idx, "index", "兜底");
            return idx.view;
        }
        // 4) 极端兜底：真实 404 模板
        return "pc_theme/" + skin + "/404";
    }

    /** 候选链（含命中信息），供调试条/后台 UI；不触发实际渲染 */
    public List<Candidate> debugChain(TemplateContext ctx) {
        String skin = activeSkin();
        List<Candidate> chain = buildChain(ctx);
        String assign = assignedTemplate(ctx);
        if (StringUtils.isNotBlank(assign)) {
            ThemeRegistry.Resolved r = registry.locate(skin, assign);
            Candidate c = new Candidate(assign, "DB指派");
            if (r != null) {
                c.exists = true;
                c.view = r.view;
                c.theme = r.skin;
            }
            chain.add(0, c);
        }
        for (Candidate c : chain) {
            ThemeRegistry.Resolved r = registry.locate(skin, c.file);
            if (r != null) {
                c.exists = true;
                c.view = r.view;
                c.theme = r.skin;
            }
        }
        return chain;
    }

    private void fill(TemplateContext ctx, ThemeRegistry.Resolved r, String file, String source) {
        ctx.setResolved(r.view);
        ctx.setResolvedTheme(r.skin);
        ctx.setResolvedFile(file + ".html");
        logger.debug("模板解析命中：{} (主题 {}, 来源 {})", r.view, r.skin, source);
    }

    // /////////////////// 候选链生成（§5） ///////////////////

    private List<Candidate> buildChain(TemplateContext ctx) {
        List<Candidate> c = new ArrayList<>();
        TemplateContext.PageType type = ctx.getPageType();
        String model = ctx.getModelCode();
        String channel = ctx.getChannelDir();
        String ch = StringUtils.isNotBlank(channel) ? channel : null;

        if (type == TemplateContext.PageType.LIST) {
            if (ch != null && StringUtils.isNotBlank(model)) {
                c.add(new Candidate("list-" + ch + "-" + model, "栏目+模型"));
            }
            if (ch != null) {
                c.add(new Candidate("list-" + ch, "栏目级"));
            }
            if (StringUtils.isNotBlank(model)) {
                c.add(new Candidate("list-" + model, "模型级"));
            }
            c.add(new Candidate("list", "通用列表"));
            if (StringUtils.isNotBlank(model)) {
                c.add(new Candidate(model + "/list", "兼容：旧模型子目录"));
            }
            c.add(new Candidate("cmodel/list", "兼容：旧通用目录"));
            c.add(new Candidate("index", "兜底"));
        } else if (type == TemplateContext.PageType.DETAIL) {
            if (StringUtils.isNotBlank(model) && ctx.getContentId() != null) {
                c.add(new Candidate("detail-" + model + "-" + ctx.getContentId(), "内容ID级"));
            }
            if (StringUtils.isNotBlank(model) && StringUtils.isNotBlank(ctx.getShortUrl())) {
                c.add(new Candidate("detail-" + model + "-" + ctx.getShortUrl(), "内容短网址级"));
            }
            if (ch != null && StringUtils.isNotBlank(model)) {
                c.add(new Candidate("detail-" + ch + "-" + model, "栏目+模型"));
            }
            if (StringUtils.isNotBlank(model)) {
                c.add(new Candidate("detail-" + model, "模型级"));
            }
            if (ch != null) {
                c.add(new Candidate("detail-" + ch, "栏目级"));
            }
            c.add(new Candidate("detail", "通用详情"));
            if (StringUtils.isNotBlank(model)) {
                c.add(new Candidate(model + "/detail", "兼容：旧模型子目录"));
            }
            c.add(new Candidate("cmodel/detail", "兼容：旧通用目录"));
            c.add(new Candidate("index", "兜底"));
        } else if (type == TemplateContext.PageType.CHANNEL_PAGE) {
            if (ch != null) {
                c.add(new Candidate("page-" + ch, "单页栏目级"));
            }
            c.add(new Candidate("page", "单页通用"));
            c.add(new Candidate("index", "兜底"));
        } else if (type == TemplateContext.PageType.SEARCH) {
            c.add(new Candidate("search", "搜索"));
            c.add(new Candidate("index", "兜底"));
        } else if (type == TemplateContext.PageType.TAG) {
            if (StringUtils.isNotBlank(ctx.getShortUrl())) {
                c.add(new Candidate("tag-" + ctx.getShortUrl(), "标签页"));
            }
            c.add(new Candidate("tag", "标签通用"));
            c.add(new Candidate("index", "兜底"));
        } else if (type == TemplateContext.PageType.ERROR) {
            int code = ctx.getErrorCode() == null ? 404 : ctx.getErrorCode();
            c.add(new Candidate(code + "", "错误页(" + code + ")"));
            c.add(new Candidate("index", "兜底"));
        } else { // INDEX
            c.add(new Candidate("index", "首页"));
        }
        return c;
    }

    // /////////////////// DB 指派（§5.3） ///////////////////

    /**
     * 查询该页面被指派的模板（相对路径）。按"内容 > 栏目 > 模型 > 站点"由具体到宽泛取第一个命中。
     */
    private String assignedTemplate(TemplateContext ctx) {
        if (assignDao == null) {
            return null;
        }
        String type = ctx.getPageType() == TemplateContext.PageType.DETAIL ? "DETAIL"
                : ctx.getPageType() == TemplateContext.PageType.LIST ? "LIST"
                : ctx.getPageType() == TemplateContext.PageType.INDEX ? "INDEX" : null;
        if (type == null) {
            return null;
        }
        // 内容级（仅详情页）
        if (ctx.getPageType() == TemplateContext.PageType.DETAIL && ctx.getContentId() != null) {
            TemplateAssign a = assignDao.findByTarget("CONTENT", String.valueOf(ctx.getContentId()), "DETAIL");
            if (a != null) {
                return a.getTemplate();
            }
        }
        // 栏目级
        if (StringUtils.isNotBlank(ctx.getChannelDir())) {
            TemplateAssign a = assignDao.findByTarget("CHANNEL", ctx.getChannelDir(), type);
            if (a != null) {
                return a.getTemplate();
            }
        }
        // 模型级
        if (StringUtils.isNotBlank(ctx.getModelCode())) {
            TemplateAssign a = assignDao.findByTarget("MODEL", ctx.getModelCode(), type);
            if (a != null) {
                return a.getTemplate();
            }
        }
        // 站点级（首页等）
        TemplateAssign a = assignDao.findByTarget("SITE", "site", type);
        return a == null ? null : a.getTemplate();
    }

    // /////////////////// 激活皮肤（含预览） ///////////////////

    /** 当前皮肤：管理员带 ?__skin=xxx 访问时返回预览主题（只影响自己，访客不受影响，§6.3） */
    public String activeSkin() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String preview = req.getParameter("__skin");
                if (StringUtils.isNotBlank(preview)) {
                    // 预览主题可能是刚创建的，缓存未命中时强制刷新一次注册表
                    if (registry.getTheme(preview) == null) {
                        registry.refresh();
                    }
                    if (registry.getTheme(preview) != null) {
                        return preview;
                    }
                }
            }
        } catch (Exception ignored) {
            // 非请求上下文（如启动期扫描）忽略
        }
        return registry.currentSkin();
    }

    /** 仅暴露当前皮肤名（供 ThemeSwitchService 等复用） */
    public String currentSkin() {
        return registry.currentSkin();
    }

    // /////////////////// 前台调试条（P8） ///////////////////

    /** ModelMap 中调试信息的键；模板 common/debug-bar.html 读取后输出 HTML 注释 */
    public static final String TPL_ATTR = "__tpl";

    /**
     * 解析并同时把命中信息写入 ModelMap，供前台调试条输出。
     * <p>与 {@link #resolve} 的唯一区别是额外暴露 {@code __tpl}；调试开关关闭时不写入，
     * 模板自然不输出任何东西（零开销、零副作用）。
     *
     * @param ctx      解析上下文
     * @param modelMap 视图模型（可为 null，此时退化为普通 resolve）
     */
    public String resolveAndExpose(TemplateContext ctx, org.springframework.ui.ModelMap modelMap) {
        String view = resolve(ctx);
        if (modelMap != null && debugEnabled()) {
            java.util.Map<String, Object> tpl = new java.util.LinkedHashMap<>();
            tpl.put("file", ctx.getResolvedFile());
            tpl.put("theme", ctx.getResolvedTheme());
            tpl.put("view", ctx.getResolved());
            tpl.put("type", ctx.getPageType() == null ? "" : ctx.getPageType().name());
            tpl.put("model", StringUtils.defaultString(ctx.getModelCode()));
            tpl.put("channel", StringUtils.defaultString(ctx.getChannelDir()));
            tpl.put("skin", activeSkin());
            // 候选链（含每一项是否存在），供浮条展开查看
            List<java.util.Map<String, Object>> chain = new ArrayList<>();
            for (Candidate c : debugChain(ctx)) {
                java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("file", c.file + ".html");
                item.put("source", c.source);
                item.put("exists", c.exists);
                item.put("hit", c.exists && ctx.getResolvedFile() != null
                        && ctx.getResolvedFile().equals(c.file + ".html")
                        && ctx.getResolvedTheme() != null && ctx.getResolvedTheme().equals(c.theme));
                chain.add(item);
            }
            tpl.put("chain", chain);
            modelMap.addAttribute(TPL_ATTR, tpl);
        }
        return view;
    }

    /**
     * 调试条总开关：系统配置 {@code template_debug}（1/true 开），默认关闭。
     * 关闭时前台输出与线上完全一致，避免任何注入风险。
     */
    public boolean debugEnabled() {
        try {
            String v = configService.getStringByKey("template_debug");
            return "1".equals(v) || "true".equalsIgnoreCase(v);
        } catch (Exception e) {
            return false;
        }
    }
}
