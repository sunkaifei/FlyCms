package com.flycms.config;

import com.flycms.interceptor.AdminInterceptor;
import com.flycms.interceptor.UserInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import java.util.Collections;
import java.util.Locale;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport{

	@Resource
	private AdminInterceptor interceptor;

	@Resource
	private UserInterceptor userInterceptor;

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}


	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver slr = new SessionLocaleResolver();
		// 默认语言
		slr.setDefaultLocale(Locale.CHINA);
		return slr;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		// 参数名
		lci.setParamName("lang");
		return lci;
	}

	//添加拦截器
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(interceptor).addPathPatterns("/system/**")
				.excludePathPatterns("/*",
						"/system/login",
						"/system/logout",
						"/system/login_act");

		registry.addInterceptor(userInterceptor).addPathPatterns("/ucenter/**","/question/add")
				.excludePathPatterns("/*",
						"/ucenter/login",
						"/ucenter/unauthorized",
						"/ucenter/reg",
						"/ucenter/mobilecode",
						"/ucenter/addMobileUser",
						"/ucenter/logout",//退出登录
						"/ucenter/login_act",//登录处理
						"/ucenter/ajaxlogin",
						"/ucenter/reg_user",
						"/ucenter/reset.json",
						"/ucenter/logintip",
						"/ucenter/mailcaptcha.json");
		registry.addInterceptor(localeChangeInterceptor());
	}

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**",
				"/*").addResourceLocations("classpath:/resources/",
				"file:./uploadfiles/",
				"file:./views/static/");
        super.addResourceHandlers(registry);
    }
}
