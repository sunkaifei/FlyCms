package com.flycms.module.article.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;
    //文章id
    private Long id;
    //短域名
    private String shortUrl;
    //用户id
    private Long userId;
    //文章标题
    private String title;
    //分类id
    private Long typeId;
    //分类所有归属id
    private String categoryId;
    //文章关键词
    private String keywords;
    //添加话题
    private String tags;
    //文章简介
    private String description;

    private Integer style;

    private String color;
    //文章内容
    private String content;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //评论次数
    private Integer countComment;
    //浏览数量
    private Integer countView;
    //推荐设置，数字越大越靠前
    private Integer recommend;
    //权重
    private Double weight;
    //内容发布时间
    private Date createTime;
    //文章最后修改时间
    private Date updateTime;
    //审核状态
    private Integer status;


}