package com.flycms.module.article.model;

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
 * @Date: 15:28 2018/9/18
 */
@Setter
@Getter
public class ArticleCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long fatherId;

    private String name;

    private String keywords;

    private String description;

    private Integer recomment;

    private Integer status;

    private Integer sort;

}
