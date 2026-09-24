package com.flycms.module.template.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板版本快照（fly_template_version，规划阶段 D）
 *
 * 每次保存都 +1 并留存 <b>历史不清理</b>：磁盘上的当前文件是"最新版"，
 * 这张表是它可以任意回退的全部历史。回滚 = 把历史内容作为新版本写入，历史不丢。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class TemplateVersion implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long templateId;
    private int version;
    private String content;
    private String remark;
    private Long editorId;
    private Date createTime;
}
