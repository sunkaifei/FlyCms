package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 字段组库（G15，复制式 Component）：字段组定义存 JSON，可应用到任意模型
 * （生成 group 字段 + 子字段复制）。复制式而非引用式——应用后各模型独立演进，
 * 避免"改模板全站联动"的意外扩散；需要同步就重新应用一份。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Component implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /** 字段组标识（应用时作为 group 字段名，须过 safeColumnName） */
    private String code;
    private String name;
    private String remark;
    /** 子字段定义数组 JSON（与模型导出 fields 同构，含 parentFieldName 语义无——均为平铺子字段） */
    private String fieldsJson;
    private Date createTime;
}
