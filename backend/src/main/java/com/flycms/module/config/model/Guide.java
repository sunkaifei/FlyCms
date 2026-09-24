package com.flycms.module.config.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 16:29 2018/7/5
 */
@Setter
@Getter
public class Guide implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 导航 id。
     * fly_guide.id 是 bigint unsigned 且无自增，必须用雪花 ID；
     * 原定义为 Integer，装不下雪花 long 值（阶段 B3 修正）。
     */
    private Long id;
    private String name;
    private String link;
    private Integer sort;
    private Integer status;
}
