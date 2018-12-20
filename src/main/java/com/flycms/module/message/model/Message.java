package com.flycms.module.message.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 站内信息实体
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */

@Setter
@Getter
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 信息id */
	private Long id;
	/** 发件人id */
    @Pattern(regexp="\\d+", message="发件人id错误")
	private Long fromId;
	/** 发件人昵称 */
	private String fromNickname;
	/** 发件人头像 */
	private String fromFace;
	/** 收件人id */
    @Pattern(regexp="\\d+", message="收件人id错误")
	private Long toId;
	/** 收件人昵称 */
	private String toNickname;
	/** 收件人头像 */
	private String toFace;
	/** 信息标题  */
	private String subject;
	/** 信息内容 */
    @NotEmpty(message="信息内容不能为空")
	private String message;
	/** 发送时间 */
	private Date sendTime;
	/** 写信时间 */
	private Date writeTime;
	/** 阅读状态  */
	private Integer hasView;
	/** 是否为系统信息 */
	private Integer isAdmin;
	/** 发送状态 0草稿，1是发送，2发送的用户删除 */
	private Integer state;

}
