package com.flycms.core.utils;

import com.flycms.constant.Const;
import com.flycms.constant.SiteConst;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 21:33 2018/9/11
 */
public class CookieUtils {
    @Autowired
    private SiteConst siteConst;

    public static String getCookie(HttpServletRequest request,String cookieName){
        Cookie[] cookies =  request.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals(cookieName)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 写入 Cookie。
     *
     * <p>安全加固（2026-10-08）：value 若含 CR/LF/;/,/空白/引号等字符，会被容器拒绝
     * （抛 IllegalArgumentException → 500）或在宽松实现下造成 Cookie 注入
     * （例如伪造出额外的会话 Cookie）。此类字符一律视为非法值并跳过写入。
     * 同时统一补 HttpOnly（本项目的自建 Cookie 都不需要 JS 读取的语义除外——
     * CSRF 的 XSRF-TOKEN 由 CsrfFilter 单独签发，不经本方法）。
     */
    public static void writeCookie(HttpServletResponse response, String cookieName,String value,Integer time){
        if (cookieName == null || cookieName.isEmpty()) {
            return;
        }
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c < 0x21 || c > 0x7E || c == ';' || c == ',' || c == '"' || c == '\\') {
                    // 非法字符：宁可不下发，也不把可控内容拼进 Set-Cookie 头
                    return;
                }
            }
        }
        Cookie cookie = new Cookie(cookieName,value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        try {
            cookie.setAttribute("SameSite", "Lax");
        } catch (IllegalArgumentException ignored) {
            // 容器不支持该属性时忽略（HttpOnly 已能挡住脚本读取）
        }
        if(time==null){
            time=3600;
        }
        cookie.setMaxAge(time);
        response.addCookie(cookie);
    }
}
