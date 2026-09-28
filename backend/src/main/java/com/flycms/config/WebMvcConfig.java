package com.flycms.config;

import com.flycms.interceptor.AdminLogInterceptor;
import com.flycms.interceptor.UserInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * MVC 配置（阶段 K3 / G3：由 {@code extends WebMvcConfigurationSupport} 回归
 * {@code implements WebMvcConfigurer}）。
 *
 * <p><b>为什么改</b>：继承 {@code WebMvcConfigurationSupport} 会<b>关闭 Spring Boot 的
 * {@code WebMvcAutoConfiguration}</b>，导致 Boot 默认的消息转换器链、静态资源缓存策略、
 * {@code spring.mvc.*} / {@code spring.web.resources.*} 配置项大部分静默失效——
 * 这正是此前必须手写 {@code super.extendMessageConverters} 与手工注册资源的原因。
 * 改为实现 {@code WebMvcConfigurer} 后由 Boot 统一装配，本项目只做"增量定制"。
 *
 * <p><b>行为等价性</b>：两个回调都保留字节级相同的逻辑（Jackson Long→String、资源目录），
 * 仅去掉对 {@code super} 的调用（{@code WebMvcConfigurer} 是回调接口，无父类实现可调）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AdminLogInterceptor adminLogInterceptor;

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
        // 旧 FreeMarker 后台已下线（views/templates/system 与 web/system 旧 Controller 一并移除），
        // 原 AdminInterceptor（/system/** 会话守卫）随之退役；API 鉴权由 ApiBaseController 统一承担。
        registry.addInterceptor(userInterceptor).addPathPatterns("/ucenter/**", "/question/add")
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
     * 将 Long 序列化为字符串（同若依 JacksonConfig 做法）。
     *
     * <p>K3 变更：去掉 {@code super.extendMessageConverters(...)}——该调用是
     * {@code WebMvcConfigurationSupport} 的父类实现（且为空），改为纯回调后无需也无法调用。
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
    }

    /**
     * 静态资源目录（K3 变更：去掉 {@code super.addResourceHandlers(...)}）。
     *
     * <p>Boot 的 {@code WebMvcAutoConfiguration} 已按
     * {@code spring.web.resources.static-locations} 注册默认静态资源处理；
     * 此处保留 {@code classpath:/resources/} 与 {@code /*} 的补充映射以维持既有可达性。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**",
                        "/*").addResourceLocations("classpath:/resources/",
                "file:./uploadfiles/",
                "file:./views/static/");
    }
}
