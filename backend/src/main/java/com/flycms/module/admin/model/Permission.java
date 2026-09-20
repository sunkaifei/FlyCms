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
    // ---- 菜单管理扩展列（若依式 M目录/C菜单/F按钮，见 doc/sql/menu-management.sql）----
    private Long parentId;
    private String menuType;
    private String menuName;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer visible;
}
