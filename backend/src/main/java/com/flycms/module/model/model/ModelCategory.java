package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 模型通用分类（fly_model_category，model_id 维度的树形分类）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class ModelCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long modelId;
    private Long fatherId;
    private String name;
    private String keywords;
    private String description;
    private int sort;
    private int status;
}
