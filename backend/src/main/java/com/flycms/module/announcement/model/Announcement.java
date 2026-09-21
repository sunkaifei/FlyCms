package com.flycms.module.announcement.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 网站公告（fly_announcement，对标 DedeCMS mynews / 帝国CMS 公告）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Announcement implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String title;
    private String content;
    private String linkUrl;
    private Date startTime;
    private Date endTime;
    private int status;
    private int sort;
    private Date createTime;
}
