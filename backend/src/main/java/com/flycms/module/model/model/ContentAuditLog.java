package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容审核日志（2026-10-23）。
 *
 * <p>背景：审核页一直把驳回原因传给 {@code /api/system/audit/audit|batch}，
 * 但两个接口的形参列表里没有 reason，Spring 只按形参绑定，原因被静默丢弃；
 * 且 {@code ModelDataService.updateStatus} 的remark 是硬编码的
 * 「状态切换→n」，审核痕迹无处可查。本实体承载审核动作，让原因真正落库。
 *
 * <p>与 {@link ContentVersion} 的分工：版本快照记「内容变成什么样」，
 * 审核日志记「谁在什么时候因为什么做了什么决定」。两者由
 * {@code updateStatusWithAudit} 一次写入。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Setter
@Getter
public class ContentAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 目标模型 code（fly_cmodel_{code}） */
    private String targetModel;
    private Long targetId;
    /** 审核时的标题快照（内容被删后仍可追溯） */
    private String title;
    /** 1=通过 2=驳回 */
    private Integer action;
    /** 驳回原因（通过时为操作备注，可空） */
    private String reason;
    private Long auditorId;
    /** 审核管理员账号快照（管理员被删后仍可追溯） */
    private String auditorName;
    private Date createTime;
}
