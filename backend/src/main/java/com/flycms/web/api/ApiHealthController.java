package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康探针（阶段 L / G8，抄 2026 REST 通行实践与 K8s probe 约定）。
 *
 * <p>两个端点语义不同，**不要混用**：
 * <ul>
 *   <li>{@code GET /api/health} —— **存活探针**：只证明进程能响应，不碰任何外部依赖。
 *       挂了才该被重启；DB 抖动不该触发重启风暴。</li>
 *   <li>{@code GET /api/ready} —— **就绪探针**：证明依赖可用（DB 连通 + 缓存可用）。
 *       不 ready 时负载均衡应把流量摘走，但**不重启**。</li>
 * </ul>
 *
 * <p>两者都**不需要登录**（探针来自 LB/K8s，没有会话）。
 * 注意：{@code /api/**} 路径版本化别名同样可用（{@code /api/v1/health}）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class ApiHealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private CacheManager cacheManager;

    /**
     * 存活探针：不查依赖，永远 200（除非进程已死）
     */
    @GetMapping("/health")
    public DataVo health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("app", "FlyCms");
        return DataVo.success(body);
    }

    /**
     * 就绪探针：DB 连通 + 缓存可用；任一失败返回 503
     */
    @GetMapping("/ready")
    public ResponseEntity<DataVo> ready() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean ok = true;

        checks.put("database", checkDatabase());
        ok &= "UP".equals(checks.get("database"));

        checks.put("cache", checkCache());
        ok &= "UP".equals(checks.get("cache"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ok ? "UP" : "DOWN");
        body.put("checks", checks);

        return ok
                ? ResponseEntity.ok(DataVo.success(body))
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(DataVo.failure("依赖未就绪", body));
    }

    private String checkDatabase() {
        if (dataSource == null) {
            return "UNKNOWN";
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkCache() {
        if (cacheManager == null) {
            return "UNKNOWN";
        }
        try {
            // 至少要有一个可用缓存区（Caffeine）
            return cacheManager.getCacheNames().isEmpty() ? "DOWN" : "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
