package com.flycms.module.admin.model;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String nickName;
    @NotEmpty(message="用户名不能为空")
    private String adminName;
    private String mobile;
    private String password;
    private String repassword;
    private int status;
    private Date createAt;
    private String ip;
    private String avatar;
    private String email;
    private Long roleId;
    private Date lastLoginTime;
    private String lastLoginIp;
    private int attempts;
    private Date attemptsTime;
}
