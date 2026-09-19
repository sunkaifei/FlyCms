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
 * @Date: 11:38 2018/9/20
 */
@Setter
@Getter
public class UserRole implements Serializable {
    private static final long serialVersionUID = 1L;
    //用户id
    private Long userId;
    //用户组id
    private Long groupId;
    //服务开始时间
    private Date serveStartTime;
    //服务结束时间
    private Date serveEndTime;
}
