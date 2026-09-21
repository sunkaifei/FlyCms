package com.flycms.module.block.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 碎片位（fly_block，规划阶段 E，对标帝国碎片管理）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Block implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String blockKey;
    private String blockName;
    /** 0富文本 1图片 2推荐位列表 3模板碎片 */
    private int blockType;
    private String content;
    private int itemCount;
    private int cacheSeconds;
    private int status;
    private int sort;
    private Date createTime;
    private Date updateTime;

    /** 运行时填充：时间窗内已启用条目（模板标签输出用，非表字段） */
    private java.util.List<BlockItem> items;
}
