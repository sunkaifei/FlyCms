package com.flycms.module.config.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
@Setter
@Getter
public class Smsapi implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    //
    private String accessKeyId;
    //
    private String accessKeySecret;
    //短信签名
    private String signName;
    //手机注册验证码
    private String regCode;
    //安全手机设置验证码
    private String safeCode;
    //密码重置验证码
    private String resetCode;
}