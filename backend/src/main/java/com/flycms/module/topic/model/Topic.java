package com.flycms.module.topic.model;

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
 * @Date: 17:55 2018/8/18
 */
@Setter
@Getter
public class Topic implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    //短域名
    private String shortUrl;
    private Integer infoType;
    private String topic;
    private String url;
    private String topicImage;
    private String content;
    private Integer countView;
    private Integer countNum;
    private Integer countFollow;
    private Integer isgood;
    private Integer status;
    private Date createTime;
}
