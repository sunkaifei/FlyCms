package com.flycms.module.brand.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
@Setter
@Getter
public class BrandCategory implements Serializable {
    private Integer id;
    @NotEmpty(message="分类名不能为空")
    private String name;

    private Integer goodsCategoryId;

    private static final long serialVersionUID = 1L;


}