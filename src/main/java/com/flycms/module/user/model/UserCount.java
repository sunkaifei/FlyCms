package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:59 2018/9/25
 */
@Setter
@Getter
public class UserCount implements Serializable {
    private static final long serialVersionUID = 1L;
    //id
    private Long userId;
    //发布问题数量
    private Integer countQuestion;
    //关注的问题数量
    private Integer countQuestionFollw;
    //关注话题数量
    private Integer countTopic;
    //回答问题数量
    private Integer countAnswer;
    //所有粉丝数量
    private Integer countFans;
    //关注数量
    private Integer countFollw;
    //发布文章数量
    private Integer countArticle;
    //发布分享数量
    private Integer countShare;
    private BigDecimal balance;
    private Integer score;
    private Integer exp;
}
