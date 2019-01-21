/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */
package com.flycms.module.template.service;

import java.io.File;

import com.flycms.module.config.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 模板工具类
 * 
 * @author sunkaifei
 * 
 */
@Service
public class TemplateService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ConfigService config;
	/**
	 * @return
	 */
	public String get403() {
		return this.getPcTemplate("403");
	}
	/**
	 * @return
	 */
	public String get404() {
		return this.getPcTemplate("404");
	}

	/**
	 * @return
	 */
	public String get500() {
		return this.getPcTemplate("500");
	}

	public String errorTips(ModelMap modelMap, String message) {
		modelMap.addAttribute("message",message);
		return this.getPcTemplate("message_tip");
	}

	/**
	 * 得到pc端样式路径
	 *
	 * @return
	 */
	public String getPcSkin() {
		return "/assets/skin/pc_theme/"+config.getStringByKey("pc_theme");
	}

	/**
	 * 得到pc端样式路径
	 *
	 * @return
	 */
	@Cacheable("default")
	public String getMobileSkin() {
		return "/assets/skin/m_theme/"+config.getStringByKey("m_theme");
	}
	/**
	 * 得到当前请求需要渲染的模板相对路径
	 * 
	 * @param template
	 * @return
	 */
	@Cacheable("default")
	public String getTemplatePath(String template) {
		return template;
	}
	
	/**
	 * 模板物理地址是否存在
	 * 
	 * @param theme
	 * @return
	 */
	@Cacheable("default")
	public Boolean isExist(String theme) {
		String themePath = "views/templates/"+ theme + ".html";
		//logger.info("模板当前路径：" + themePath);
		File file = new File(themePath);
		if (file.exists()) {
			return true;
		} else {
			logger.info("模板不存在：" + file.getAbsolutePath());
			return false;
		}
	}
	
	/**
	 * 得到通用模板
	 * 
	 * @param template
	 * @return
	 * @throws Exception
	 */
	public String getPcTemplate(String template) {
		String themePath = "pc_theme/"+config.getStringByKey("pc_theme")+"/"+template;
		if (this.isExist(themePath)) {
			return this.getTemplatePath(themePath);
		}
		logger.warn("模板文件不存在！！");
		return this.getTemplatePath("pc_theme/"+config.getStringByKey("pc_theme")+"/404");
	}

	public String getAdminTemplate(String template) {
		String themePath = "system/"+template;
		if (this.isExist(themePath)) {
			return this.getTemplatePath(themePath);
		}
		logger.warn("模板文件不存在！！");
		return this.getTemplatePath("pc_theme/"+config.getStringByKey("pc_theme")+"/404");
	}
	
}
