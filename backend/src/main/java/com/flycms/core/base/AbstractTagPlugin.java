package com.flycms.core.base;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import com.flycms.core.utils.StringHelperUtils;
import org.apache.commons.lang3.StringUtils;
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

	/**
	 * 标签日志器（P3-5）。
	 *
	 * <p>历史遗留问题：56 个标签类里有 20 多个写了
	 * {@code catch (Exception e) { env.setVariable("x_page", wrap(null)); }}——
	 * 把查询异常<b>静默降级为空数据</b>。后果是"SQL 报错"与"确实没数据"在页面上
	 * 长得一模一样，排查只能靠猜。本字段与 {@link #logTagFailure} 提供统一留痕入口。
	 */
	protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

	@Autowired
	protected HttpServletRequest request;

	@Autowired
	private FreeMarkerConfigurer freeMarkerConfigurer;

	/**
	 * 标签执行失败统一留痕：保持"降级为空数据不炸整页"的既有行为，
	 * 但必须留下可检索的日志（含标签名与原因），否则"为什么这块是空的"无迹可查。
	 *
	 * @param tagName 标签调用名，如 {@code fly_article_page}
	 */
	protected void logTagFailure(String tagName, Exception e) {
		logger.warn("标签 <@{}> 执行失败，已降级为空数据：{}", tagName, e == null ? "未知异常" : e.getMessage());
	}

	@Override
	@PostConstruct
	public void init() throws TemplateModelException {
		String className = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1);
		String beanName = StringUtils.uncapitalize(className);
		String tagName = "fly_" + StringHelperUtils.toUnderline(beanName);
		// 阶段 K5/G5：原实现为 getApplicationContext().getBean(beanName) —— 在自身 @PostConstruct 中
		// 向容器反查自身，形成 `xxxModel -> xxxModel` 自循环（此前靠 allow-circular-references 侥幸启动）。
		// 这里要注册的就是当前实例，直接用 this 即可；标签类无 AOP 代理，this 与容器内实例一致。
		freeMarkerConfigurer.getConfiguration().setSharedVariable(tagName, this);
		logger.info("[tag-reg] {} -> {} (@{})", tagName, this.getClass().getName(), Integer.toHexString(System.identityHashCode(this)));
	}

}
