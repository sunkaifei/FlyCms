package com.flycms.module.article.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 22:11 2018/10/21
 */
@Setter
@Getter
public class ArticleVotes implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //用户ID
    private Long userId;
    //内容类型，0文章，1评论内容
    private Integer infoType;
    //信息id
    private Long infoId;
    //顶
    private Integer digg;
    //踩
    private Integer burys;
    //添加时间
    private Date createTime;
}
