package com.flycms.module.other.model;

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
 * @Date: 20:41 2018/11/4
 */
@Setter
@Getter
public class FilterKeyword implements Serializable {
    private static final long serialVersionUID = 1L;
    //id
    private Long id;
    //违禁关键词
    private String keyword;
}
