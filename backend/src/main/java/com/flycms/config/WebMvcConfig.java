package com.flycms.config;

import com.flycms.interceptor.AdminLogInterceptor;
import com.flycms.interceptor.UserInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
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
     *
     * <p><b>主题静态资源必须显式声明缓存策略</b>（2026-10-08 修「改了模板却整页错乱」）：
     * 原先 {@code /skin/**} 只由默认资源处理兜底，响应里<b>既没有 {@code Cache-Control}
     * 也没有 {@code ETag}</b>，只有 {@code Last-Modified}。没有显式新鲜度时浏览器按启发式规则
     * 自行推算（约为 {@code 10% × 距 Last-Modified 的时长}）——主题长期不动时该时长可达数小时，
     * 于是改版后客户端**拿不到新 CSS 也不会来校验**：新 HTML 配旧 CSS。
     * 实测首页新 HTML 用到的 66 个 class 里有 34 个（52%）在旧 CSS 中根本不存在
     * （{@code .hero-sub} / {@code .foot-col} / {@code .qa-row} / {@code .sec-white} …），
     * 表现就是「排版全是乱的」，且每次改版都更严重。
     *
     * <p>{@code no-cache} 不等于不缓存：副本仍会存，但每次请求都带
     * {@code If-Modified-Since} 回源校验，未变则 304——既保证改动立刻生效，也不浪费带宽。
     * 与之配合，模板侧给资源 URL 带 {@code ?v=${theme.version}}（见 {@code common/header.html}
     * / {@code common/footer.html}）：换 URL 才能让<b>已经持有旧副本</b>的浏览器重新拉取
     * （仅加响应头无法打破已缓存副本，因为浏览器根本不会发请求）。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**",
                        "/*").addResourceLocations("classpath:/resources/",
                "file:./uploadfiles/",
                "file:./views/static/");

        // 主题皮肤资源：/skin/** 是当前主题约定，/assets/skin/** 是 WordPress 转换主题的约定
        registry.addResourceHandler("/skin/**", "/assets/**")
                .addResourceLocations("file:./views/static/skin/",
                        "file:./views/static/assets/",
                        "classpath:/resources/skin/",
                        "classpath:/resources/assets/")
                .setCacheControl(CacheControl.noCache());
    }
}
