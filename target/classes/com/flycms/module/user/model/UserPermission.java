package com.flycms.module.user.model;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class UserPermission implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String actionKey;
    private String controller;
    private String remark;
}
