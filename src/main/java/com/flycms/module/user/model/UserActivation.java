package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 20:28 2018/8/26
 */
@Setter
@Getter
public class UserActivation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userName;
    private String code;
    private Integer codeType;
    private Date referTime;
    private Integer referStatus;
}
