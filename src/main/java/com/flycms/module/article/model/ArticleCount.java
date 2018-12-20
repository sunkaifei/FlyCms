package com.flycms.module.article.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 文章统计实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 8:43 2018/10/26
 */
@Setter
@Getter
public class ArticleCount implements Serializable {
    private static final long serialVersionUID = 1L;
    //文章id
    private Long articleId;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //评论次数
    private Integer countComment;
    //浏览数量
    private Integer countView;
    //权重
    private Double weight;
}
