package com.flycms.module.form.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 表单字段（规划阶段 F）
 *
 * fieldType 复用自定义模型的 FieldTypeEnum（15 种），不另造一套类型体系。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class FormField implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long formId;
    /** 字段标识，提交 JSON 的 key */
    private String fieldCode;
    /** 字段名称 */
    private String fieldName;
    /** 字段类型，取值同 FieldTypeEnum */
    private String fieldType;
    /** 是否必填 0否 1是 */
    private Integer required;
    private String defaultValue;
    private String placeholder;
    /** select/radio/checkbox 选项，换行分隔 */
    private String options;
    private Integer sort;
}
