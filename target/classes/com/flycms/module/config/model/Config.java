package com.flycms.module.config.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class Config implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private Integer typebase;
    private String keycode;
    private String keyvalue;
    private String description;
    private Integer sort;
}
