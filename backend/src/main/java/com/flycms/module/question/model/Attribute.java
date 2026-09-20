package com.flycms.module.question.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class Attribute implements Serializable {
    private Long id;

    private Long modelId;

    private Integer type;

    private String name;

    private Integer search;

    private String value;

    private static final long serialVersionUID = 1L;

}