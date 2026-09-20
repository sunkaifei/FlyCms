package com.flycms.module.question.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-8-18
 */
@Setter
@Getter
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //短域名
    private String shortUrl;
    private Long userId;
    private String title;
    private String content;
    //最佳答案奖励金币数量
    private Integer score;
    //该话题浏览数量
    private Integer countView;
    //关注该话题数量
    private Integer countFollow;
    //评论数量
    private Integer countAnswer;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //推荐设置，数字越大越靠前
    private Integer recommend;
    //权重
    private Double weight;
    private Date createTime;
    private Integer status;
}
