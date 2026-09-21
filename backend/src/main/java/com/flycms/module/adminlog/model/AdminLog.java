package com.flycms.module.adminlog.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理操作审计（fly_admin_log，规划阶段 A3）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class AdminLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long adminId;
    private String adminName;
    private String method;
    private String path;
    private String query;
    private String ip;
    private Integer status;
    private Integer costMs;
    private Date createTime;
}
