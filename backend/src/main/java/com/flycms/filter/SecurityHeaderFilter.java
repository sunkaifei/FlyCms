package com.flycms.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全响应头过滤器（阶段 K2 / G2，P1）。
 *
 * <p>补齐 OWASP 2026 建议的基础安全头：
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} —— 禁止 MIME 嗅探；</li>
 *   <li>{@code X-Frame-Options: SAMEORIGIN} —— 防点击劫持（后台禁被嵌套）；</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}；</li>
 *   <li>{@code Content-Security-Policy} —— **默认策略刻意保守**（只禁嵌套/插件对象/篡改 base），
 *       不设 {@code default-src 'self'}：存量前台模板含内联脚本与外部 CDN，强 CSP 会直接白屏，
 *       违反本方案"不毁存量是红线"的约定。可用 {@code flycms.security.csp.policy} 覆盖；</li>
 *   <li>{@code Strict-Transport-Security} —— 仅 HTTPS 请求下发（HTTP 下下发无意义且会被浏览器忽略）。</li>
 * </ul>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeaderFilter extends OncePerRequestFilter {

    /** 保守 CSP：不限制脚本/样式来源，只做"防嵌套 + 禁插件 + 锁 base"三件事 */
    @Value("${flycms.security.csp.policy:frame-ancestors 'self'; object-src 'none'; base-uri 'self'}")
    private String cspPolicy;

    /** HSTS 有效期（秒），0 表示不下发 */
    @Value("${flycms.security.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${flycms.security.hsts.include-subdomains:true}")
    private boolean hstsIncludeSubDomains;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        if (cspPolicy != null && !cspPolicy.isBlank()) {
            response.setHeader("Content-Security-Policy", cspPolicy);
        }
        if (hstsMaxAge > 0 && isSecureRequest(request)) {
            String value = "max-age=" + hstsMaxAge;
            if (hstsIncludeSubDomains) {
                value += "; includeSubDomains";
            }
            response.setHeader("Strict-Transport-Security", value);
        }
        chain.doFilter(request, response);
    }

    /**
     * 判断是否 HTTPS——容器直连看 {@code isSecure()}，反向代理看 {@code X-Forwarded-Proto}
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.toLowerCase().contains("https");
    }
}
