package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：开源之家<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:07 2018/8/21
 */

@Setter
@Getter
public class UserInvite implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //被邀请人ID
    private Long toUserId;
    //邀请人ID
    private Long formUserId;
    //状态
    private Integer status;
    private Date createTime;

}
