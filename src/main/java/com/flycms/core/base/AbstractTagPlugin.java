package com.flycms.core.base;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.flycms.core.utils.StringHelperUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModelException;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 标签解析抽象类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */

@Service
public abstract class AbstractTagPlugin extends ApplicationObjectSupport implements TemplateDirectiveModel, Plugin {
	@Autowired
	protected HttpServletRequest request;

	@Autowired
	private FreeMarkerConfigurer freeMarkerConfigurer;

	@Override
	@PostConstruct
	public void init() throws TemplateModelException {
		String className = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1);
		String beanName = StringUtils.uncapitalize(className);
		String tagName = "fly_" + StringHelperUtils.toUnderline(beanName);
		freeMarkerConfigurer.getConfiguration().setSharedVariable(tagName, this.getApplicationContext().getBean(beanName));
	}

}
