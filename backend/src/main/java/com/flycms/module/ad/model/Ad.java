package com.flycms.module.ad.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 广告（fly_ad）。类型 image 图片 / text 文字 / code 代码；
 * 投放时间窗为空表示不限；点击经 /ad/click/{id} 计数后 302 跳转。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Ad implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 所属广告位 */
    private Long positionId;
    private String name;
    /** 类型：image 图片 / text 文字 / code 代码 */
    private String adType;
    /** 图片地址（image 类型，直存 URL） */
    private String imageUrl;
    /** 跳转链接 */
    private String url;
    /** 文字内容（text 类型） */
    private String textContent;
    /** 代码（code 类型，原样输出） */
    private String htmlCode;
    /** 权重（越大越靠前） */
    private Integer weight;
    /** 开始时间（空=立即） */
    private Date startTime;
    /** 结束时间（空=永久） */
    private Date endTime;
    /** 1=启用 0=停用 */
    private int status;
    /** 展示次数 */
    private Long countView;
    /** 点击次数 */
    private Long countClick;
    private String remark;
    private int sort;
    private Date createTime;
    private Date updateTime;
}
