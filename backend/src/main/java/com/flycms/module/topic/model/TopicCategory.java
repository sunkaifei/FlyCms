package com.flycms.module.topic.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * <b>话题分类实体类</b>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:26 2018/9/7
 */
@Setter
@Getter
public class TopicCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long fatherId;
    private String name;
    private String customUrl;
    private String keywords;
    private String description;
    private Integer isRecommend;
    private Integer sort;
    private Integer status;
}
