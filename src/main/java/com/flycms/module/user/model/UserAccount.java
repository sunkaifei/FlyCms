package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 用户账户信息实体
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:36 2018/9/11
 */
@Setter
@Getter
public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id ;
    //用户id
    private Integer userId;
    private BigDecimal balance;
    private Integer score;
    private Integer exp;
}
