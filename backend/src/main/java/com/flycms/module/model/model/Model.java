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
    /** 所属分组（① 组织层：NULL=未分组；分组聚合菜单与管理页展示，不动存储语义） */
    private Long groupId;
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
    /**
     * 内容图片本地化（Q3）：1=保存内容时自动抓取编辑器（content）里的外站图片到本地，
     * 登记 fly_images（进附件库/引用计数）并把 img src 替换为本地地址；
     * 替换域名取站点参数 fly_img_domain（空=相对路径/当前域名）。DB 缺省 0 关闭。
     */
    private Integer localizeImages;
    /**
     * 后台可新增开关（2026-10-02）：0=内容仅前台生成——后台隐藏该模型的内容管理菜单与
     * 「添加内容」入口、modelData 保存端点兜底拒绝；列表/审核/编辑/删除保留。
     * 与 enableSubmit 组合成四象限（常规/纯前台/动作模型/双向）。DB 缺省 1（现状不变）。
     */
    private Integer adminCreate;
    /** 发布表单页签顺序（JSON 数组，如 ["基础信息","扩展信息"]；空=按字段出现顺序） */
    private String formTabs;
    /** 发布表单默认打开的页签名（空=第一个页签） */
    private String formDefaultTab;
    private Date createTime;
    private Date updateTime;
}
