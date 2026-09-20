package com.flycms.module.score.model;
import com.flycms.module.user.model.User;
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
 * 积分模块--积分记录实体类
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
public class ScoreDetail implements Serializable {

	private static final long serialVersionUID = 1L;
	
    private Long id;
    private String type;
    private Long userId;
    private User user;
    private Integer score;
    /** 变更后剩余积分 **/
    private Integer balance;
    private String remark;
    private Long foreignId;
    private Long scoreRuleId;
    private Date createTime;
    private Integer status;

}