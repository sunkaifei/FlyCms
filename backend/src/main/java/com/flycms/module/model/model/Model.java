package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容模型定义（fly_model）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Model implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String code;
    private String titleLabel;
    private int isSystem;
    private String listTemplate;
    private String detailTemplate;
    private String icon;
    private String description;
    private int sort;
    private int status;
    /**
     * 表单布局开关（U3）：0=内容表单不渲染「详细内容」富文本选项卡
     * （链接/导航/留言类模型没有正文），保存时忽略 content。
     * Integer 判空更新：未传 = 不变更（DB 缺省 1）。
     */
    private Integer useContent;
    /** 表单布局开关（U3）：0=内容表单不渲染「SEO 设置」选项卡。DB 缺省 1。 */
    private Integer useSeo;
    /**
     * E9 模型级特性开关：0=该模型关闭评论（前台发表与评论标签均拒绝），DB 缺省 1 开启。
     */
    private Integer enableComment;
    /**
     * V2 前台投稿开关：0=关闭该模型前台投稿（/ucenter/submit/{code} 与投稿页均 404 语义拒绝），
     * DB 缺省 1 开启。
     */
    private Integer enableSubmit;
    private Date createTime;
    private Date updateTime;
}
