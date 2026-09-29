package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容版本快照（M/G12，对标 Payload versions+drafts / Directus Revisions）。
 *
 * <p>与模板版本的差异：模板版本是「文件为事实源 + DB 存快照」；内容版本反过来——
 * <b>DB 为事实源，快照即版本</b>。保存链路：写 {@code fly_content_version}（version+1）→
 * 已由调用方更新主表。恢复 = 把快照内容写回主表并<b>另存为新版本</b>，历史不丢。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class ContentVersion implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 目标模型 code（fly_cmodel_{code}） */
    private String targetModel;
    private Long targetId;
    /** 版本号，同一内容从 1 递增 */
    private Integer version;
    /** 整行快照（含自定义字段，JSON） */
    private String contentJson;
    /** 快照时主行状态（0待审 1发布 2未通过…） */
    private Integer status;
    /** 操作管理员 id */
    private Long editorId;
    private String remark;
    private Date createTime;
}
