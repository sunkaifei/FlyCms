package com.flycms.module.ai.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容向量（G21 语义搜索）：标题+正文按段落分块向量化后落 MySQL，
 * 检索时 Java 侧余弦——小站规模免向量库；内容量到十万级再迁 ES/pgvector。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Embedding implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String targetModel;
    private Long targetId;
    private Integer chunk;
    private String vectorJson;
    private String embedModel;
    private Date updateTime;
}
