package com.flycms.module.admin.model;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String actionKey;
    private String controller;
    private String remark;
}
