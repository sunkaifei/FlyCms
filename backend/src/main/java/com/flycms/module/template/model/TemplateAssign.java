package com.flycms.module.template.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板指派（规划 §5.3 / §12.1）。对应 {@code fly_template_assign}。
 *
 * <p>DB 只存"谁用哪个模板"的<b>关系</b>，不存模板正文（文件系统恒为事实源，规避 WP "模板存 DB 导致站点依赖数据库"的坑）。
 * 解析时 {@code target_type/target_id/page_type} 命中的指派优先级最高（与 WP 后台 Page Template 下拉一致）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class TemplateAssign implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** CONTENT / CATEGORY / CHANNEL / MODEL / SITE */
    private String targetType;
    /** 内容ID / 分类ID / 栏目目录 / 模型code / "site" */
    private String targetId;
    /** LIST / DETAIL / INDEX */
    private String pageType;
    /** 模板相对路径，如 detail-product.html（解析时由 ThemeRegistry 定位真实主题） */
    private String template;
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
