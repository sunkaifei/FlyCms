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
    private Integer id;
    private String name;
    private String link;
    private Integer sort;
    private Integer status;
}
