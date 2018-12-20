package com.flycms.module.share.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@Setter
@Getter
public class Share implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //短域名
    private String shortUrl;
    private Long typeId;
    private String categoryId;
    private String title;
    private String content;
    //添加话题
    private String tags;
    private Long userId;
    private Integer needmoney;
    private String downloads;
    //是否匿名，0否，1是
    private Integer hide;
    //浏览次数
    private Integer countView;
    //下载次数
    private Integer countDownloads;
    //评论次数
    private Integer countComment;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //销售数量
    private Integer countSum;
    //推荐设置，数字越大越靠前
    private Integer recommend;
    //权重
    private Integer weight;
    //添加时间
    private Date createTime;
    //最后更新时间
    private Date updateTime;
    //审核状态，0未审核 1正常状态 2审核未通过 3删除
    private Integer status;
}