package com.flycms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CSRF 防护配置（阶段 K2 / G2）。
 *
 * <p>密钥：{@code flycms.security.csrf.*}
 *
 * <p><b>灰度策略</b>：{@code enabled} 默认 <b>false（监听模式）</b>——过滤器仍会签发/校验并
 * <b>记录</b>异常请求，但不拦截。这样存量前端（尚未带 token）不会被打断；
 * 观察一段时间确认无漏网端点后，把 {@code enabled} 置 true 切强制。
 * 这与《主流CMS对标与全项目优化开发方案》§7.2 第 2 条的"先记录不拦截"一致。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "flycms.security.csrf")
public class CsrfConfig {

    /** 是否强制校验（false = 监听模式，仅日志） */
    private boolean enabled = false;

    /** 前端回填的请求头名 */
    private String headerName = "X-XSRF-TOKEN";

    /** 下发给前端（JS 可读）的 Cookie 名 */
    private String cookieName = "XSRF-TOKEN";

    /** 生效路径前缀（只保护 API，前台表单走自身逻辑） */
    private String pathPrefix = "/api/";

    /** token 长度（十六进制字符数） */
    private int tokenLength = 32;
}
