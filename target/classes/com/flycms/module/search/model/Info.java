package com.flycms.module.search.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 21:10 2018/9/19
 */
@Setter
@Getter
public class Info {
    //solrID
    private String id;
    //短域名
    private String shortUrl;
    //用户id
    private Long userId;
    //信息标题
    private String title;
    //信息id
    private Long infoId;
    //信息类型
    private Integer infoType;
    //信息分类搜索id
    private String categoryId;
    //信息内容
    private String content;
    //信息添加时间
    private String createTime;
}
