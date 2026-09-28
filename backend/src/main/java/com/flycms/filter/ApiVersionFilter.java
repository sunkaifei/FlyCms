package com.flycms.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 版本化过滤器（阶段 L / G6，抄 WordPress {@code /wp/v2} 的路径版本约定）。
 *
 * <p>把 {@code /api/v1/**} 在进入 DispatcherServlet 之前**改写**为 {@code /api/**}，
 * 从而在不改 21 个控制器、不动前端的前提下让 v1 成为稳定别名。
 *
 * <p><b>为什么用重写而不是 {@code @RequestMapping({"/api","/api/v1"})} 双注册</b>：
 * {@code PermissionService.getSyncAllPermission()} 会遍历 {@code RequestMappingHandlerMapping}
 * 把每个映射登记成 {@code fly_admin_permission} 行——双注册会让每个端点**重复登记两条权限**，
 * 污染权限表并让角色勾选界面出现成对重复项。重写在映射层之前完成，权限表保持单份。
 *
 * <p>顺序：必须在 CSRF/安全头之后、DispatcherServlet 之前（见 {@code @Order}）。
 * CSRF 只校验 URI 前缀是否为 {@code /api/}，{@code /api/v1/...} 同样命中，互不影响。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class ApiVersionFilter extends OncePerRequestFilter {

    /** 版本化前缀 */
    private static final String VERSION_PREFIX = "/api/v1";
    /** 内部真实前缀 */
    private static final String BASE_PREFIX = "/api";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !(uri.equals(VERSION_PREFIX) || uri.startsWith(VERSION_PREFIX + "/"))) {
            chain.doFilter(request, response);
            return;
        }

        final String rewritten = BASE_PREFIX + uri.substring(VERSION_PREFIX.length());
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return rewritten;
            }

            @Override
            public String getServletPath() {
                return rewritten;
            }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = new StringBuffer();
                String scheme = getScheme();
                int port = getServerPort();
                if (port < 0) {
                    port = "https".equals(scheme) ? 443 : 80;
                }
                url.append(scheme).append("://").append(getServerName());
                if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                    url.append(':').append(port);
                }
                url.append(rewritten);
                return url;
            }
        }, response);
    }
}
