package com.flycms.module.ad.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 广告位（fly_ad_position）。模板以 ad_key 调用：<@fly_ad key="banner_top">。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class AdPosition implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 广告位名称（如：首页轮播） */
    private String name;
    /** 调用标识（模板 <@fly_ad key="...">） */
    private String adKey;
    /** 描述/投放位置说明 */
    private String description;
    /** 建议宽度(px) */
    private Integer width;
    /** 建议高度(px) */
    private Integer height;
    private int sort;
    /** 1=启用 0=停用（停用后前台不输出） */
    private int status;
    private Date createTime;
    private Date updateTime;
    /** 位下广告数（列表联查冗余） */
    private Long adCount;
}
