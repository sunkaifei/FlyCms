package com.flycms.module.dict.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典数据（fly_dict_data）。dict_label 表单显示，dict_value 入库存储。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class DictData implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 所属字典类型键 */
    private String dictType;
    /** 标签（表单显示） */
    private String dictLabel;
    /** 键值（入库存储） */
    private String dictValue;
    private String remark;
    private int sort;
    /** 1=启用 0=停用（停用后不出现在候选项） */
    private int status;
    private Date createTime;
    private Date updateTime;
}
