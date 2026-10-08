package com.flycms.module.channel.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 统一栏目（fly_channel，规划 §8 阶段 C）
 *
 * <b>设计要点（规避 §6.5"栏目与模型绑死"）</b>：
 * model_id 是<b>默认值而非约束</b>——栏目可以不绑模型（单页/外链/聚合），
 * 聚合栏目还能混排多个模型；删除栏目只删栏目行，内容数据一律不动。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Channel implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 父栏目，0=根 */
    private Long fatherId;
    private String channelName;
    /** URL 目录名，全站唯一 */
    private String channelDir;
    /** 绑定模型，0=不绑定 */
    private Long modelId;
    /** 0列表 1单页 2外链 3聚合 */
    private int channelType;
    private String pageContent;
    private String outUrl;
    private String listTemplate;
    private String detailTemplate;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private int pageSize;
    /** 0隐藏 1显示 */
    private int status;
    private int sort;
    private java.util.Date createTime;
    private java.util.Date updateTime;

    /** 运行时字段：子栏目（树接口填充，非表列） */
    private List<Channel> children;
    /** 运行时字段：绑定模型的 code（前台按模型拼链接 / 导航去重用，来自左连 fly_model） */
    private String modelCode;
    /** 运行时字段：绑定模型的名称（前台给「子栏目」这类列表做可读标签，来自左连 fly_model） */
    private String modelName;
}
