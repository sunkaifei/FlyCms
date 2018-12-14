package com.flycms.module.question.model;

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
 * @Date: 23:10 2018/8/18
 */
@Setter
@Getter
public class Images implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 图片编号 */
    public Integer id;
    /**
     * 类型id
     * 0.话题和评论话题，1.评论，2.相册
     */
    public Integer channelId;
    /**  图片的对应内容id */
    public Integer tid;
    /** 用户id */
    public Integer userId;
    /** 相册id */
    public Integer picid;
    /** 图片地址 */
    public String imgurl;
    /** 图片名称 */
    public String imgname;
    /** 图片说明 */
    public String description;
    /** 图片体积大小 */
    public String filesize;
    /**  图片宽度 */
    public String imgWidth;
    /**  图片高度 */
    public String imgHeight;
    /**  图片指纹 */
    public String signature;
    /** 添加时间 */
    public String addTime;
    /** 删除设置 */
    public String imgDelete;
    /** 排序 */
    public Integer sort;
}
