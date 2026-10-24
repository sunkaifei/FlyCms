package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户站内信（2026-10-23）。
 *
 * <p>背景：审核页一直弹「已驳回，并已站内信通知作者」，但在此之前全库
 * 只有一处注释提及站内信，无表、无实现、无接口 —— UI 承诺了不存在的能力。
 * 本实体 + {@code fly_user_message} 让那句话第一次成为真的。
 *
 * <p>投递语义：<b>一行 = 一个接收者一条消息</b>。广播由 Service 展开成 N 行，
 * 不设「user_id=0 表示全体」的特例 —— 否则已读状态无法按人区分。
 *
 * <p>审核通知会回填 {@code targetModel}/{@code targetId}，前台可直接跳回被驳回的内容。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Setter
@Getter
public class UserMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息类型 */
    public interface Type {
        /** 系统通知（后台手工发送） */
        int SYSTEM = 1;
        /** 审核通知（通过/驳回） */
        int AUDIT = 2;
        /** 互动提醒（预留） */
        int INTERACT = 3;
    }

    private Long id;
    private Long userId;
    /** 见 {@link Type} */
    private Integer msgType;
    private String title;
    /** 纯文本正文：前台一律转义输出，不接受 HTML */
    private String content;
    /** 关联模型 code（审核通知回填，可空） */
    private String targetModel;
    /** 关联内容 id（审核通知回填，可空） */
    private Long targetId;
    /** 0未读 1已读 */
    private Integer isRead;
    /** 发送管理员 id；0/NULL 表示系统 */
    private Long senderId;
    /** 发送者账号快照（管理员被删后仍可追溯） */
    private String senderName;
    private Date createTime;
    private Date readTime;
}
