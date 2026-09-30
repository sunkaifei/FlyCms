package com.flycms.module.dict.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典类型（fly_dict_type）。字段 select/radio/checkbox 可绑定 dict_type，
 * 发布/筛选表单按字典渲染候选项（field.options 为回退数据源）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class DictType implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 字典名称（如：运行平台） */
    private String dictName;
    /** 字典类型键（如 app_os），fly_model_field.dict_type 绑定用 */
    private String dictType;
    private String remark;
    private int sort;
    /** 1=启用 0=停用（停用后表单回退 field.options） */
    private int status;
    private Date createTime;
    private Date updateTime;
}
