package com.flycms.core.utils;

import com.flycms.constant.Const;
import com.flycms.constant.SiteConst;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    public static void writeCookie(HttpServletResponse response, String cookieName,String value,Integer time){
        Cookie cookie = new Cookie(cookieName,value);
        cookie.setPath("/");
        if(time==null){
            time=3600;
        }
        cookie.setMaxAge(time);
        response.addCookie(cookie);
    }
}
