package com.flycms.module.template.model;

/**
 * 模板解析上下文（规划 §4.3）：解析引擎的唯一输入，取代散落的参数传递。
 *
 * <p>字段语义与文档 §4.3 一致：
 * <ul>
 *   <li>{@code pageType}：页面类型（INDEX/LIST/DETAIL/CHANNEL_PAGE/SEARCH/TAG/ERROR）</li>
 *   <li>{@code modelCode}：模型 code，如 articles</li>
 *   <li>{@code channelDir}：栏目目录名，如 news</li>
 *   <li>{@code contentId}：内容 ID（详情页）</li>
 *   <li>{@code shortUrl}：短网址（详情页）</li>
 *   <li>{@code errorCode}：错误码（403/404/500）</li>
 * </ul>
 *
 * <p>DB 指派（{@code fly_template_assign}）优先级最高，对应 WordPress 后台 Page Template 下拉。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class TemplateContext {

    /** 页面类型枚举 */
    public enum PageType {
        INDEX, LIST, DETAIL, CHANNEL_PAGE, SEARCH, TAG, ERROR
    }

    private PageType pageType;
    private String modelCode;
    private String channelDir;
    private Long contentId;
    private String shortUrl;
    private Integer errorCode;

    /**
     * 调用方显式指定的模板文件（如栏目表 {@code fly_channel.list_template}），
     * 优先级高于 DB 指派与候选链——"我就是要这个模板"是比"按规则挑"更强的意图。
     * <p>命中失败（文件不存在）时静默回退候选链，不抛异常：配置错误不该让页面 500。
     */
    private String overrideFile;

    // 解析结果（由 TemplateResolver 回填，供调试条/UI 使用）
    private String resolved;       // 最终视图名，如 pc_theme/corp/list-news
    private String resolvedTheme;  // 命中的主题（子主题覆盖时为父主题）
    private String resolvedFile;   // 命中的相对文件，如 list-news.html

    public TemplateContext() {
    }

    public static TemplateContext list(String modelCode, String channelDir) {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.LIST;
        c.modelCode = modelCode;
        c.channelDir = channelDir;
        return c;
    }

    public static TemplateContext detail(String modelCode, String shortUrl, Long contentId, String channelDir) {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.DETAIL;
        c.modelCode = modelCode;
        c.shortUrl = shortUrl;
        c.contentId = contentId;
        c.channelDir = channelDir;
        return c;
    }

    public static TemplateContext channelPage(String channelDir) {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.CHANNEL_PAGE;
        c.channelDir = channelDir;
        return c;
    }

    public static TemplateContext index() {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.INDEX;
        return c;
    }

    public static TemplateContext search() {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.SEARCH;
        return c;
    }

    public static TemplateContext tag(String tag) {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.TAG;
        c.shortUrl = tag; // 复用 shortUrl 字段承载 tag 名
        return c;
    }

    public static TemplateContext error(int code) {
        TemplateContext c = new TemplateContext();
        c.pageType = PageType.ERROR;
        c.errorCode = code;
        return c;
    }

    public PageType getPageType() {
        return pageType;
    }

    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getChannelDir() {
        return channelDir;
    }

    public void setChannelDir(String channelDir) {
        this.channelDir = channelDir;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getResolved() {
        return resolved;
    }

    public void setResolved(String resolved) {
        this.resolved = resolved;
    }

    public String getResolvedTheme() {
        return resolvedTheme;
    }

    public void setResolvedTheme(String resolvedTheme) {
        this.resolvedTheme = resolvedTheme;
    }

    public String getResolvedFile() {
        return resolvedFile;
    }

    public void setResolvedFile(String resolvedFile) {
        this.resolvedFile = resolvedFile;
    }

    public String getOverrideFile() {
        return overrideFile;
    }

    public void setOverrideFile(String overrideFile) {
        this.overrideFile = overrideFile;
    }

    /** 链式设置显式模板（可空；空值等价于不指定） */
    public TemplateContext withOverride(String file) {
        this.overrideFile = file;
        return this;
    }
}
