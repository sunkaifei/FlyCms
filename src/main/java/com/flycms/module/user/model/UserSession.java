package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 20:22 2018/11/8
 */
@Setter
@Getter
public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;
    //用户sessionKey
    private String sessionKey;
    //用户组id
    private Long userId;
    //服务开始时间
    private long expireTime;
    //最后更新时间
    private Date updateTime;

}
