package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 模型字段定义（fly_model_field）。columnType 由后端 FieldTypeEnum 推导冗余存储，前端不可传。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class ModelField implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long modelId;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String columnType;
    private String defaultValue;
    private Integer maxlength;
    private String dictType;
    private String options;
    private int isRequired;
    private int isList;
    private int isSearch;
    private int isFilter;
    private String regex;
    private String placeholder;
    private String tips;
    private int sort;
    private int status;
    private Date createTime;
    private Date updateTime;
}
