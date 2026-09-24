package com.flycms.module.form.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 表单数据（规划阶段 F）
 *
 * 统一表 + data_json 存储：表单数据只用于后台查看与导出，
 * 无需真实列与索引，JSON 足够且避免动态建表数量失控。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class FormData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long formId;
    /** 提交用户，0=匿名 */
    private Long userId;
    private String ip;
    /** 字段值 JSON */
    private String dataJson;
    /** 0待审 1正常 2不通过（表单 audit=0 时直接为 1） */
    private Integer status;
    private Date createTime;

    /** 非表字段：所属表单编码（列表展示用） */
    private String formCode;
    /** 非表字段：所属表单名称（列表展示用） */
    private String formName;
}
