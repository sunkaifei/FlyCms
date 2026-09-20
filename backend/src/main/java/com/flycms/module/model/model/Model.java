package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容模型定义（fly_model）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Model implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String code;
    private String titleLabel;
    private int isSystem;
    private String listTemplate;
    private String detailTemplate;
    private String icon;
    private String description;
    private int sort;
    private int status;
    private Date createTime;
    private Date updateTime;
}
