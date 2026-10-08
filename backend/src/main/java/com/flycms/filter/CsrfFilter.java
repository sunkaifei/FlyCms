package com.flycms.filter;

import com.flycms.config.CsrfConfig;
import com.flycms.core.entity.ErrorVo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * CSRF 双提交 Cookie 过滤器（阶段 K2 / G2，P1）。
 *
 * <p>背景：认证完全依赖 Cookie（{@code JSESSIONID}），浏览器自动携带的特性使所有
 * {@code /api/**} 的 POST/PUT/DELETE 天然暴露于 CSRF。CORS 的 {@code allowCredentials(true)}
 * 进一步放宽了可利用面。
 *
 * <p>方案（双提交 Cookie，不依赖 Spring Security，避免引入重型依赖）：
 * <ol>
 *   <li>任意 GET 请求若未带 token Cookie，签发一个随机 token 并以 <b>非 HttpOnly</b> 写入 Cookie
 *       （JS 必须能读，这是双提交模型的前提）；</li>
 *   <li>非安全方法（POST/PUT/PATCH/DELETE）命中 {@code /api/**} 时，要求请求头
 *       {@code X-XSRF-TOKEN} 与 Cookie 值一致；</li>
 *   <li>{@code enabled=false}（默认，监听模式）时不一致只记日志放行；置 true 后返回 403 结构化 JSON。</li>
 * </ol>
 *
 * <p>攻击者无法读取跨站 Cookie（同源策略），因此无法伪造出匹配的头——这就是双提交 Cookie 的防护原理。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CsrfFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEX = "0123456789abcdef";

    private final CsrfConfig config;

    public CsrfFilter(CsrfConfig config) {
        this.config = config;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean insideApi = config.getPathPrefix() == null || config.getPathPrefix().isEmpty()
                || uri.startsWith(config.getPathPrefix());

        String method = request.getMethod();
        boolean safe = "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)
                || "TRACE".equals(method);

        String cookieToken = readCookie(request);

        if (safe) {
            // 任何「非静态资源」的安全请求都签发 token，而不只限 /api/**：
            // 否则 SPA 首屏由 nginx 直出、首次交互就是 POST /api/auth/login 时，
            // 客户端手里根本没有 token cookie，强制校验会把登录本身打死。
            if ((cookieToken == null || cookieToken.isEmpty()) && !looksLikeStaticAsset(uri)) {
                issueToken(request, response);
            }
            chain.doFilter(request, response);
            return;
        }

        // 非 API 路径的写方法（如前台 /ucenter/** 表单）不在本过滤器职责内
        if (!insideApi) {
            chain.doFilter(request, response);
            return;
        }

        if (cookieToken == null || cookieToken.isEmpty()) {
            // 客户端尚无 token cookie：此时浏览器也没有可被跨站利用的会话凭据
            // （登录前请求属此列），签发后放行，避免"第一次登录必被 403"。
            issueToken(request, response);
            chain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(config.getHeaderName());
        boolean valid = cookieToken.equals(headerToken);
        if (valid) {
            chain.doFilter(request, response);
            return;
        }

        String traceId = TraceIdFilter.currentTraceId();
        if (!config.isEnabled()) {
            // 监听模式：只记录，不拦截（灰度观察期）
            logger.warn("CSRF 监听：{} {} 缺少/不匹配 {}（cookie=有，header={}）[traceId={}]",
                    method, uri, config.getHeaderName(),
                    headerToken == null ? "无" : "有", traceId);
            chain.doFilter(request, response);
            return;
        }

        logger.warn("CSRF 拒绝：{} {} token 校验失败 [traceId={}]", method, uri, traceId);
        ErrorVo vo = ErrorVo.internal("CSRF 校验失败，请刷新页面后重试", uri, traceId);
        vo.setStatus(HttpServletResponse.SC_FORBIDDEN);
        vo.setCode(ErrorVo.CODE_FORBIDDEN);
        vo.setMessage("CSRF 校验失败，请刷新页面后重试");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(vo));
    }

    /** 签发双提交 token cookie（非 HttpOnly：JS 必须能读出来回填请求头） */
    private void issueToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(config.getCookieName(), randomToken());
        cookie.setPath("/");
        cookie.setHttpOnly(false);   // 双提交模型必须让前端可读
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(-1);        // 会话级
        response.addCookie(cookie);
    }

    /** 静态资源（末段含扩展名，如 .js/.png）不签发 token，避免污染静态响应与缓存 */
    private boolean looksLikeStaticAsset(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        int slash = uri.lastIndexOf('/');
        String last = slash < 0 ? uri : uri.substring(slash + 1);
        return last.indexOf('.') > 0;
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (config.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String randomToken() {
        int len = Math.max(16, config.getTokenLength());
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(HEX.charAt(RANDOM.nextInt(HEX.length())));
        }
        return sb.toString();
    }
}
