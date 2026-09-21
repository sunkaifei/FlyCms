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
import org.apache.commons.lang3.StringUtils;

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
            SnowFlake snowFlake = new SnowFlake(2, 3);
            log.setId(snowFlake.nextId());
            log.setAdminId(admin.getId());
            log.setAdminName(StringUtils.defaultString(admin.getAdminName()));
            log.setMethod("POST");
            log.setPath(StringUtils.abbreviate(request.getRequestURI(), 200));
            log.setQuery(StringUtils.abbreviate(request.getQueryString(), 500));
            log.setIp(IpUtils.getIpAddr(request));
            log.setStatus(response.getStatus());
            log.setCostMs((int) cost);
            log.setCreateTime(new Date());

            AdminLog dao = log;
            LOG_EXECUTOR.execute(() -> {
                try {
                    adminLogDao.insertLog(dao);
                } catch (Exception ignored) {
                    // 审计写失败不影响主流程
                }
            });
        } catch (Exception ignored) {
        } finally {
            START_TIME.remove();
        }
    }

    @PreDestroy
    public void shutdown() {
        LOG_EXECUTOR.shutdown();
    }
}
