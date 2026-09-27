package com.flycms.interceptor;

import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.core.utils.IpUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.adminlog.dao.AdminLogDao;
import com.flycms.module.adminlog.model.AdminLog;
import com.flycms.module.admin.model.Admin;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 管理操作审计拦截器（规划阶段 A3）：
 * 对 /api/**、/system/** 的 POST，记录管理员、路径、参数摘要（500 截断）、IP、耗时、响应码。
 * 只记录已登录管理员的操作（前台匿名提交不进审计）；异步写避免拖慢请求。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
public class AdminLogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminLogInterceptor.class);

    private static final ExecutorService LOG_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "admin-log-writer");
                t.setDaemon(true);
                return t;
            });

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Autowired
    private AdminLogDao adminLogDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        START_TIME.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                return;
            }
            Admin admin = AdminSessionUtils.getLoginMember(request);
            if (admin == null) {
                return;
            }
            Long start = START_TIME.get();
            long cost = start == null ? 0 : System.currentTimeMillis() - start;

            AdminLog log = new AdminLog();
            SnowFlake snowFlake = SnowFlake.getInstance();
            log.setId(snowFlake.nextId());
            log.setAdminId(admin.getId());
            log.setAdminName(StringUtils.defaultString(admin.getAdminName()));
            log.setMethod("POST");
            log.setPath(StringUtils.abbreviate(request.getRequestURI(), 200));
            log.setQuery(StringUtils.abbreviate(paramSummary(request), 500));
            log.setIp(IpUtils.getIpAddr(request));
            log.setStatus(response.getStatus());
            log.setCostMs((int) cost);
            log.setCreateTime(new Date());

            AdminLog dao = log;
            LOG_EXECUTOR.execute(() -> {
                try {
                    adminLogDao.insertLog(dao);
                } catch (Exception e) {
                    // 审计写失败不影响主流程，但必须留痕，否则"审计丢记录"无从发现
                    logger.warn("后台审计日志写入失败（path={}）：{}", dao.getPath(), e.getMessage());
                }
            });
        } catch (Exception e) {
            // 组装审计对象阶段异常（如 IP 解析、Session 读取）：不影响主流程，仅留痕
            logger.warn("后台审计日志组装失败：{}", e.getMessage());
        } finally {
            START_TIME.remove();
        }
    }

    /**
     * 参数摘要：queryString + form-body 参数（Servlet 容器已解析的表单参数）。
     * 老实现只取 getQueryString()，而后台接口几乎全是 form POST → 审计的 query 恒为空，
     * 审计失去取证价值。这里用 getParameterMap() 兜住 body，并对敏感键脱敏。
     * 注意：不读 request.getInputStream()，避免破坏后续 @RequestParam 绑定。
     */
    private String paramSummary(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String qs = request.getQueryString();
        if (StringUtils.isNotBlank(qs)) {
            sb.append(qs);
        }
        try {
            Map<String, String[]> map = request.getParameterMap();
            if (map != null) {
                for (Map.Entry<String, String[]> e : map.entrySet()) {
                    String key = e.getKey();
                    String[] values = e.getValue();
                    String joined = values == null ? "" : String.join(",", values);
                    // 单个值超长（富文本正文）只留长度标记，避免撑爆 500 字摘要
                    String shown = joined.length() > 200
                            ? "[len:" + joined.length() + "]"
                            : joined;
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    sb.append(key).append('=').append(mask(key, shown));
                }
            }
        } catch (Exception ignored) {
            // multipart 等场景下取参数可能抛异常，忽略并保证主流程不受影响
        }
        return sb.toString();
    }

    /** 敏感字段脱敏：密码/令牌一类不落审计明文 */
    private String mask(String key, String value) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("pass") || lower.contains("pwd") || lower.contains("secret")
                || lower.contains("token") || lower.contains("captcha")) {
            return "******";
        }
        return value;
    }

    @PreDestroy
    public void shutdown() {
        LOG_EXECUTOR.shutdown();
    }
}
