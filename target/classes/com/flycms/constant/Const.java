package com.flycms.constant;


import com.flycms.module.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 变量设置类
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
@Service
public class Const {
	@Autowired
	protected ConfigService configService;

	public static final String SYSTEM_NAME = "FlyCms";
	public static final String SYSTEM_VERSION = "1.0.0";
	/**
	 * 验证码
	 */
	public static String KAPTCHA_SESSION_KEY = "kaptcha";

	/**
	 * 管理员用户后台Session
	 */
	public static final String SESSION_ADMIN = "session_admin";

	/**
	 * 前台用户Session
	 */
	public static final String SESSION_USER = "session_user";

	public static final String  UPLOAD_PATH = "./uploadfiles";  //结尾不要带/

}
