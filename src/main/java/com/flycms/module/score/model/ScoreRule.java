package com.flycms.module.score.model;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 积分模块--积分规则实体类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年10月15日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0<br/>
 * 
 */
@Setter
@Getter
public class ScoreRule implements Serializable  {
	private static final long serialVersionUID = 1L;
	
    private Long id;
    private String name;
    private Integer score;
    private String remark;
    private String type;
    private Integer status;
    private Date updateTime;
    private Date createTime;
}
