package com.flycms.module.block.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 碎片条目（fly_block_item，支持定时上下线）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class BlockItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long blockId;
    private String title;
    private String image;
    private String url;
    private String summary;
    private Date startTime;
    private Date endTime;
    private int status;
    private int sort;
    private Date createTime;
}
