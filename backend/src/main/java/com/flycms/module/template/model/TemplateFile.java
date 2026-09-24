package com.flycms.module.template.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板登记（fly_template，规划阶段 D）
 *
 * <b>决策：文件系统为事实源，DB 只登记与存版本快照。</b>
 * 渲染链路（Freemarker loader 指向文件路径）零改动，并保留"FTP/手工兜底修改"的运维后门；
 * 版本快照解决 §6.2"模板写坏不可回滚"与 §6.3"调试成本高"。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class TemplateFile implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 所属皮肤目录名 */
    private String skin;
    /** 皮肤内相对路径，如 index.html、news/list.html */
    private String filePath;
    private Date updateTime;
    private Long editorId;
}
