package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 模型分组（fly_model_group，① 组织层）：一个业务模块（商城/问答）由多张表配合，
 * 分组把归属同一模块的模型聚合出统一入口——菜单按分组生成子目录、模型管理页聚合展示。
 * 分组不动存储语义（一模型一表不变），分组删除/停用只影响组织呈现。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class ModelGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String icon;
    private String description;
    private int sort;
    private int status;
    private Date createTime;

    /** 组内模型（管理页聚合展示用，不入库） */
    private List<Model> models;
}
