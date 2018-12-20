package com.flycms.module.topic.model;

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
 * @Date: 14:56 2018/9/13
 */
@Setter
@Getter
public class TopicInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long infoId;
    private Long topicId;
    private Long userId;
    private Integer infoType;
}
