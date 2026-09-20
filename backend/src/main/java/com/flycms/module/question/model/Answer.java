package com.flycms.module.question.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 18:43 2018/8/20
 */
@Setter
@Getter
public class Answer implements Serializable {
    private static final long serialVersionUID = 1L;
    //评论id
    private Long id;
    //问题id
    private Long questionId;
    //评论用户id
    private Long userId;
    //添加时间
    private Date createTime;
    //添加时间
    private Date lastTime;
    //审核状态
    private Integer status;
    //权重计算值
    private BigDecimal weight;
    //评论内容
    private String content;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //评论数量
    private Integer countComment;
}
