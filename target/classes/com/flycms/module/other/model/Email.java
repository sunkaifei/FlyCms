package com.flycms.module.other.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@Setter
@Getter
public class Email implements Serializable {
    private static final long serialVersionUID = 1L;
    //用户id
    private Integer id;
    //模板标记码
    private String tpCode;
    //邮件标题
    private String title;
    //邮件内容
    private String content;
}