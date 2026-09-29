package com.flycms.module.favorite.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 15:13 2018/9/6
 */
@Setter
@Getter
public class Favorite implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Integer infoType;
    private Long infoId;
    /** E7 收藏平台化：目标模型 code（NULL=遗留文章收藏，等价 infoType=1） */
    private String modelCode;
    private Date createTime;
}