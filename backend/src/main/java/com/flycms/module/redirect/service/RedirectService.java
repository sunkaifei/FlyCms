package com.flycms.module.redirect.service;

import com.flycms.core.utils.SnowFlake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * SEO 重定向规则（T-b，对标 Yoast/Redirection 插件）：
 * 来源路径 → 目标地址 301。规则经 Cache（"redirect" 区）缓存，
 * 增删改清缓存；未命中返回 null 由调用方走原 404 流程。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);

    @Autowired
    private com.flycms.module.redirect.dao.RedirectDao redirectDao;

    /** 查来源路径的启用规则映射（缓存全表，规则量小；key=source_path 小写归一） */
    @Cacheable(value = "redirect", key = "'all'")
    public Map<String, String> allEnabled() {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : redirectDao.allEnabledRows()) {
            Object src = row.get("src");
            Object target = row.get("target");
            if (src != null && target != null) {
                map.put(String.valueOf(src), String.valueOf(target));
            }
        }
        return map;
    }

    /** 命中即返回目标地址，未命中 null */
    public String match(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Map<String, String> all = allEnabled();
        if (all.isEmpty()) {
            return null;
        }
        String key = path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1) : path;
        return all.get(key) != null ? all.get(key) : all.get(key.toLowerCase());
    }

    public void save(String sourcePath, String targetUrl, int status) {
        String src = normalize(sourcePath);
        String existing = redirectDao.findIdBySource(src);
        if (existing != null) {
            redirectDao.update(existing, targetUrl, status);
        } else {
            redirectDao.insert(SnowFlake.getInstance().nextId(), src, targetUrl, status);
        }
        evict();
    }

    public void delete(String id) {
        redirectDao.delete(id);
        evict();
    }

    @CacheEvict(value = "redirect", key = "'all'")
    public void evict() {
        log.info("重定向规则已更新，缓存已清");
    }

    private String normalize(String p) {
        String s = p == null ? "" : p.trim();
        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        if (s.length() > 1 && s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
