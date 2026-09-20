package com.flycms.module.brand.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class Brand  implements Serializable {
    private String name;

    private String logo;

    private String url;

    private int sort;

    private String categoryIds;

    private String description;

    private static final long serialVersionUID = 1L;

}