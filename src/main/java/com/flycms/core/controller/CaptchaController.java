package com.flycms.core.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.flycms.core.utils.captcha.Captcha;
import com.flycms.core.utils.captcha.GifCaptcha;
import com.flycms.core.utils.captcha.SpecCaptcha;
import com.flycms.constant.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 *  网站验证Controller
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */

@Controller
public class CaptchaController {
	private static Logger logger = LoggerFactory.getLogger(CaptchaController.class);
	/**
	 * 获取验证码（Gif版本）
	 * @param response
	 */
	@GetMapping(value="/captcha/default")
	public void getGifCode(HttpServletResponse response,HttpServletRequest request,HttpSession session){
		try {
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Pragma", "No-cache");
	        response.setHeader("Cache-Control", "no-cache");  
	        response.setDateHeader("Expires", 0);  
	        response.setContentType("image/gif");  
	        /**
	         * gif格式动画验证码
	         * 宽，高，位数。
	         */
	        Captcha captcha = new GifCaptcha(146,33,4);
	        //输出
	        captcha.out(response.getOutputStream()); 
	        //存入Session
	        session.setAttribute(Const.KAPTCHA_SESSION_KEY,captcha.text().toLowerCase());
		} catch (Exception e) {
			logger.error("获取gif验证码异常：%s",e.getMessage());
		}
	}
	
	
	/**
	 * 获取验证码（jpg版本）
	 * @param response
	 */
	@GetMapping(value="getJPGCode")
	public void getJPGCode(HttpServletResponse response,HttpServletRequest request){
		try {
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Pragma", "No-cache");  
			response.setHeader("Cache-Control", "no-cache");  
			response.setDateHeader("Expires", 0);  
			response.setContentType("image/jpg");  
			/**
			 * jgp格式验证码
			 * 宽，高，位数。
			 */
			Captcha captcha = new SpecCaptcha(146,33,4);
			//输出
			captcha.out(response.getOutputStream());
			HttpSession session = request.getSession(true);  
			//存入Session
			session.setAttribute(Const.KAPTCHA_SESSION_KEY,captcha.text().toLowerCase());
		} catch (Exception e) {
			logger.error("获取jpg验证码异常：%s",e.getMessage());
		}
	}
}
