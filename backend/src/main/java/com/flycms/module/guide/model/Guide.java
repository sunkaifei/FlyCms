package com.flycms.module.guide.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 站点导航项（fly_guide）。
 *
 * <p>三项来源：type=0 自定义链接（link 直存）；type=1 栏目（refId=fly_channel.id，
 * 链接渲染为 /{channelDir}/）；type=2 模型分类（refId=fly_model_category.id，
 * 链接渲染为 /{modelCode}/c{refId}）。url 为运行时计算的最终链接，不入库。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Guide implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 类型：自定义链接 */
    public static final int TYPE_LINK = 0;
    /** 类型：栏目 */
    public static final int TYPE_CHANNEL = 1;
    /** 类型：模型分类 */
    public static final int TYPE_CATEGORY = 2;

    private Long id;
    private Long fatherId;
    private String name;
    private int type;
    private Long refId;
    private String link;
    /** 打开方式：_blank 新窗口，空为当前窗口 */
    private String target;
    private int sort;
    private int status;
    private java.util.Date createTime;
    private java.util.Date updateTime;

    /** 树输出（children 恒为 [] 而非 null，前端表格树无需判空） */
    private List<Guide> children;

    /** 运行时计算的最终链接（type=1/2 由栏目目录/模型分类拼出），不入库 */
    private String url;

    /** type=2 时绑定的模型 code（编辑回显分类树用），不入库 */
    private String refModel;
}
