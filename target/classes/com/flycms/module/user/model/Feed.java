package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:38 2018/9/14
 */
@Setter
@Getter
public class Feed implements Serializable {
    private static final long serialVersionUID = 1L;
    //id
    private Long id;
    //用户id
    private Long userId;
    //信息类型，0问题，1文章，2分享
    private Integer infoType;
    //信息id
    private Long infoId;

}
