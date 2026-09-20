package com.flycms.module.share.model;

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
 * @Date: 16:08 2018/10/31
 */
@Setter
@Getter
public class ShareCount implements Serializable {
    private static final long serialVersionUID = 1L;
    //问题id
    private Integer shareId;
    //下载数量
    private Integer countDownloads;
    //评论人数
    private Integer countComment;
    //顶数量
    private Integer countDigg;
    //踩人数
    private Integer countBurys;
    //浏览数量
    private Integer countView;
    //权重
    private Double weight;
}
