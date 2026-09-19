package com.flycms.module.question.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 问题统计实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:07 2018/10/29
 */
@Setter
@Getter
public class QuestionCount implements Serializable {
    private static final long serialVersionUID = 1L;
    //问题id
    private Long questionId;
    //回答数量
    private Integer countAnswer;
    //关注人数
    private Integer countFollow;
    //浏览数量
    private Integer countView;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //权重
    private Double weight;
}
