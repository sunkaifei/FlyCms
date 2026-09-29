package com.flycms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * <p>
 * 原实现基于 EhCache 2（ehcache.xml），Spring Boot 3+ 已移除 EhCache 2 支持，
 * 迁移为 Caffeine，语义保持一致：
 * - longterm/config/user/areas/question/share：永久缓存（原 eternal=true）
 * - article：120 秒未访问过期（原 timeToIdleSeconds=120）
 * - 其余动态创建的缓存（如 message）：120 秒未访问过期（原 defaultCache）
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 永久缓存名，对应原 ehcache.xml 中 eternal=true 的缓存
     */
    private static final String[] ETERNAL_CACHES = {
            "longterm", "config", "user", "areas", "question", "share"
    };

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 默认规格：动态创建的缓存 120 秒过期
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterAccess(120, TimeUnit.SECONDS)
                .maximumSize(10000));
        for (String name : ETERNAL_CACHES) {
            if ("user".equals(name)) {
                // user 缓存必须带过期：findByUsername 会把"用户不存在"的 null 一并缓存，
                // 永不过期会导致新注册用户对查询不可见直到重启（规划阶段 A 修复）
                cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                        .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
                        .maximumSize(20000)
                        .build());
                continue;
            }
            cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                    .maximumSize(20000)
                    .build());
        }
        cacheManager.registerCustomCache("article", Caffeine.newBuilder()
                .expireAfterAccess(120, TimeUnit.SECONDS)
                .maximumSize(2000)
                .build());
        return cacheManager;
    }
}
