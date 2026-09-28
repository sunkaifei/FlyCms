package com.flycms.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器（阶段 K1 / G1）。
 *
 * <p>为每个请求生成/透传一个 traceId：
 * <ul>
 *   <li>优先取请求头 {@code X-Trace-Id}（网关/上游已生成时保持同一条链路）；</li>
 *   <li>否则本地生成 16 位随机串；</li>
 *   <li>写入 MDC（key {@code traceId}），随 log4j2 的 {@code %X{traceId}} 落到日志；
 *       若未配置 pattern 变量也不影响功能；</li>
 *   <li>回写响应头 {@code X-Trace-Id}，前端报错可直接抄给后端定位。</li>
 * </ul>
 *
 * <p>{@link org.springframework.boot.web.servlet.FilterRegistrationBean} 未显式声明的
 * {@code Filter} Bean 会被 Boot 自动注册，{@code @Order} 决定其在过滤器链中的位置。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** MDC key */
    public static final String MDC_KEY = "traceId";
    /** 请求/响应头名 */
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 供异常处理器读取当前 traceId（异步线程取不到 MDC 时返回 null，由调用方兜底）
     */
    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }
}
