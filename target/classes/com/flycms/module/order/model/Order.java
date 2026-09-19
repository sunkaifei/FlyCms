package com.flycms.module.order.model;

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
 * 订单实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:52 2018/9/11
 */
@Setter
@Getter
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long shareId;
    private Long userId;
    private Integer status;
    private Date createTime;
}