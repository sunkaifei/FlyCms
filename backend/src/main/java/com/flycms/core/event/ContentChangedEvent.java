package com.flycms.core.event;

import org.springframework.context.ApplicationEvent;

import java.util.Date;
import java.util.List;

/**
 * G18 内容变更事件（插件/自动化的稳定契约，本期只发布不消费）。
 *
 * <p>发布点：ModelDataService 的 insert/update/delete/updateStatus 与 CommentService 的
 * 发表/审核/删除。未来「事件型插件」（§7.14）或自动化规则（G17）订阅本事件即可切入内容生命周期，
 * 无需改动核心服务。监听侧约定：<b>不得抛异常阻断主流程</b>（发布侧已 try/catch，监听异常不回滚内容写入）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class ContentChangedEvent extends ApplicationEvent {

    private final String action;
    private final String modelCode;
    private final List<Long> ids;
    private final Long operatorId;
    private final Date time;

    public ContentChangedEvent(Object source, String action, String modelCode, List<Long> ids, Long operatorId) {
        super(source);
        this.action = action;
        this.modelCode = modelCode;
        this.ids = ids;
        this.operatorId = operatorId;
        this.time = new Date();
    }

    /** insert / update / delete / status / comment_add / comment_audit / comment_delete */
    public String getAction() {
        return action;
    }

    /** 目标模型 code（评论事件为目标内容所属模型） */
    public String getModelCode() {
        return modelCode;
    }

    public List<Long> getIds() {
        return ids;
    }

    /** 操作人（前台用户 id 或管理端 admin id，发布方语义） */
    public Long getOperatorId() {
        return operatorId;
    }

    public Date getTime() {
        return time;
    }
}
