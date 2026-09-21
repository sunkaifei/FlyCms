package com.flycms.config;

import com.flycms.interceptor.AdminInterceptor;
import com.flycms.interceptor.UserInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.http.converter.HttpMessageConverter;

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
	private com.flycms.interceptor.AdminLogInterceptor adminLogInterceptor;

	@Resource
	private UserInterceptor userInterceptor;

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

		// 管理操作审计（阶段 A3）：/api/** 与 /system/** 的 POST，仅记录已登录管理员
		registry.addInterceptor(adminLogInterceptor).addPathPatterns("/api/**", "/system/**");
	}

    /**
     * Long → String：雪花 ID 超出 JS Number.MAX_SAFE_INTEGER，以 number 下发前端会
     * 丢失末位精度导致按 id 查询全部落空。统一替换 MVC 的 Jackson 转换器，
     * 将 Long 序列化为字符串（同若依 JacksonConfig 做法；Boot 4 的
     * JsonMapperBuilderCustomizer 实测不影响 MVC 默认转换器，故在此直接替换）。
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        tools.jackson.databind.module.SimpleModule module =
                new tools.jackson.databind.module.SimpleModule("flycms-long-to-string");
        module.addSerializer(Long.class, tools.jackson.databind.ser.std.ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, tools.jackson.databind.ser.std.ToStringSerializer.instance);
        tools.jackson.databind.json.JsonMapper mapper =
                tools.jackson.databind.json.JsonMapper.builder().addModule(module).build();
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof org.springframework.http.converter.json.JacksonJsonHttpMessageConverter) {
                converters.set(i, new org.springframework.http.converter.json.JacksonJsonHttpMessageConverter(mapper));
            }
        }
        super.extendMessageConverters(converters);
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
