package com.flycms.module.article.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
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
 * @Date: 15:34 2018/10/19
 */
@Setter
@Getter
public class ArticleComment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    //文章id
    private Long articleId;
    //评论归属id，默认为0
    private Long parentId;
    //用户ID
    private Long userId;
    //评论内容
    @NotEmpty(message="请填写评论内容！")
    private String content;
    //添加时间
    private Date createTime;
    //顶
    private Integer countDigg;
    //踩
    private Integer countBurys;
    //审核状态
    private Integer status;
}
