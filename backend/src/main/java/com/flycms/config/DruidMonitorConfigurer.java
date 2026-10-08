package com.flycms.config;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid 监控配置。
 *
 * <p><b>安全口径（2026-10-08 加固）</b>：原实现把监控台无条件注册到 {@code /druid/*}
 * 并把账号口令硬编码在源码里（公开仓库可直接读到），同时白名单写死
 * {@code 127.0.0.1}——生产常见部署是「nginx 与本服务同机反代」，
 * 此时 {@code request.getRemoteAddr()} 恒为 127.0.0.1，白名单形同虚设，
 * 互联网可直达监控台查看 SQL、URI、会话等信息。现改为：
 * <ol>
 *   <li><b>默认不注册</b>：仅在 {@code flycms.druid.enabled=true} 且账号口令均已配置
 *       （来自配置/环境变量，源码内不再有默认口令）时才注册；</li>
 *   <li>生产 profile 显式保持关闭；</li>
 *   <li>{@code allow} 收敛到真实运维出口网段（默认仅本机，且要求反代传递真实来源）；</li>
 *   <li>修正 {@link WebStatFilter} 的挂载路径（原 {@code /admin/*} 与 {@code /druid/*}
 *       不匹配，SQL 统计实际上收不到数据）。</li>
 * </ol>
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Configuration
public class DruidMonitorConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(DruidMonitorConfigurer.class);

    /** 监控台账号，无默认值（禁止硬编码口令；开启与否由 {@code flycms.druid.enabled} 控制） */
    @Value("${flycms.druid.username:}")
    private String monitorUsername;

    /** 监控台口令，无默认值；为空则整个监控台不注册 */
    @Value("${flycms.druid.password:}")
    private String monitorPassword;

    /** 允许访问监控台的来源 IP（逗号分隔），默认仅本机 */
    @Value("${flycms.druid.allow:127.0.0.1}")
    private String allow;

    /**
     * 注册 Druid 监控台 Servlet。
     *
     * <p>注意：这里必须用 {@code @ConditionalOnProperty} 做条件装配，<b>不能</b>在
     * {@code @Bean} 方法里 {@code return null}——Spring 会把 null 包装成 NullBean，
     * 而 {@code ServletContextInitializerBeans} 要求该类型的 Bean 必须真的是
     * ServletContextInitializer，否则启动直接抛
     * {@code BeanNotOfRequiredTypeException: ... actually of type NullBean}。
     */
    @Bean
    @ConditionalOnProperty(name = "flycms.druid.enabled", havingValue = "true")
    public ServletRegistrationBean<StatViewServlet> registrationBean() {
        if (StringUtils.isBlank(monitorUsername) || StringUtils.isBlank(monitorPassword)) {
            // 显式开启了监控台却没给账号口令：宁可启动失败也不要暴露一个弱口令入口
            throw new IllegalStateException(
                    "flycms.druid.enabled=true 时必须同时配置 flycms.druid.username 与 "
                            + "flycms.druid.password（口令不得硬编码在源码中）");
        }
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<StatViewServlet>(new StatViewServlet(), "/druid/*");
        // 白名单：默认仅本机。若部署在反向代理之后，务必改为运维真实出口网段，
        // 否则 remoteAddr 恒为代理地址，白名单失去意义。
        bean.addInitParameter("allow", allow);
        bean.addInitParameter("loginUsername", monitorUsername);
        bean.addInitParameter("loginPassword", monitorPassword);
        // 禁止通过监控台重置统计/清空数据
        bean.addInitParameter("resetEnable", "false");
        logger.warn("Druid 监控台已启用：/druid/*（allow={}），请确认该入口未暴露到公网", allow);
        return bean;
    }

    /**
     * SQL/URI 统计过滤器。
     *
     * <p>原实现 {@code addUrlPatterns("/admin/*")} 与 {@code StatViewServlet} 的
     * {@code /druid/*} 并不匹配，导致监控台看不到 SQL 统计数据；改为全局挂载，
     * 并排除静态资源与监控台自身，避免无谓开销与统计噪声。
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> druidStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean =
                new FilterRegistrationBean<WebStatFilter>(new WebStatFilter());
        bean.addUrlPatterns("/*");
        bean.addInitParameter("exclusions",
                "*.js,*.gif,*.jpg,*.jpeg,*.png,*.css,*.ico,*.woff,*.woff2,/druid/*");
        return bean;
    }
}
