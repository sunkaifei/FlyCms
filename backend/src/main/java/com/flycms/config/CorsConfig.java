package com.flycms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置（阶段 K2 / G2，P1）。
 *
 * <p><b>改造前的问题</b>：白名单硬编码在代码里（含生产域名），且对白名单主机放行
 * {@code http://host:*} 任意端口 + {@code allowCredentials(true)}——生产环境把
 * {@code localhost} 也放行了，等于给本地恶意页面开了带凭据的跨域口子。
 *
 * <p><b>改造后</b>：白名单走配置 {@code flycms.cors.allowed-hosts}（逗号分隔的<b>主机名</b>，不带协议端口），
 * 由 profile 决定内容——默认（dev）只放行本机，prod profile 只放行生产域名（见 application.yml）。
 * 主机仍按 {@code http(s)://host:*} 模式放行任意端口，以兼容前端 dev server 端口漂移。
 */
@Configuration
public class CorsConfig {

    /**
     * 允许跨域的主机名白名单（不带协议与端口）。
     * 例：{@code localhost,127.0.0.1}；空值表示不启用 CORS（纯同源部署）。
     */
    @Value("${flycms.cors.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHosts;

    /**
     * 把主机名展开为 4 条 origin pattern：
     * {@code http://host:*}、{@code https://host:*}、{@code http://host}、{@code https://host}。
     *
     * <p>历史 Bug 备忘：仅用 {@code addAllowedOrigin("http://localhost")} 时，
     * Origin 必须精确匹配"协议+域名+端口"，前端 dev 服务跑在 5666 等端口会不匹配而被拦，
     * 故对白名单主机统一放行任意端口。
     */
    private void addAllowedOrigins(CorsConfiguration corsConfiguration, List<String> hosts) {
        for (String host : hosts) {
            corsConfiguration.addAllowedOriginPattern("http://" + host + ":*");
            corsConfiguration.addAllowedOriginPattern("https://" + host + ":*");
            corsConfiguration.addAllowedOriginPattern("http://" + host);
            corsConfiguration.addAllowedOriginPattern("https://" + host);
        }
    }

    /**
     * 项目加载时生成 CORS 过滤器统一管理跨源请求。
     *
     * <p>用 {@link FilterRegistrationBean} 显式注册并置于过滤器链最前（仅次于 TraceId），
     * 保证预检 OPTIONS 在任何鉴权/CSRF 逻辑之前被处理。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        List<String> hosts = Arrays.stream(allowedHosts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        addAllowedOrigins(corsConfiguration, hosts);
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.setAllowCredentials(true);   // 允许携带凭据（JSESSIONID）
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        // 空白名单时不注册（纯同源部署场景）
        registration.setEnabled(!hosts.isEmpty());
        return registration;
    }
}
