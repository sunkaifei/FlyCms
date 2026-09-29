package com.flycms.module.comment.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台评论（E4，多态）：按 target_model + target_id 引用任意自定义模型内容，
 * 替代退役的 fly_article_comment / fly_share_comment 分模块实现。
 *
 * <p>status 语义沿用既有评论口径：0 待审 / 1 通过 / 2 未通过。
 * 先审后显开关复用 config 键 {@code fly_comment_audit}（1=先审后显，落 status=0）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 目标模型 code（articles/topics/…，写入前过 SqlSafeUtil.safeModelCode） */
    private String targetModel;
    private Long targetId;
    private Long userId;
    /** 父评论 id，0 = 顶层评论 */
    private Long parentId;
    private String content;
    /** 0 待审 1 通过 2 未通过 */
    private Integer status;
    private Date createTime;
}
