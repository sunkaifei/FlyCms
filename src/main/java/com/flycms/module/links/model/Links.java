package com.flycms.module.links.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 友情链接实体
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Setter
@Getter
public class Links implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private int type;
	@NotEmpty(message="请填写网站名称！")
	private String linkName;
	@NotEmpty(message="请填写网站网址！")
	private String linkUrl;
	private String linkLogo;
	private int isShow;
	private int sort;
	//创建日期
	private Date createTime;
}
