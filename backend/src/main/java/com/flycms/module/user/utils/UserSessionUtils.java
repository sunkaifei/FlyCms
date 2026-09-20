package com.flycms.module.user.utils;

import com.flycms.constant.Const;
import com.flycms.constant.SiteConst;
import com.flycms.core.utils.Md5Utils;
import com.flycms.module.user.model.User;
import com.flycms.module.user.model.UserSession;
import com.flycms.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Date;

/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 用户登录信息操作
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年5月25日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0 <br/>
 * 
 */
@Slf4j
@Service
public class UserSessionUtils {

    @Autowired
    private UserService userService;
    @Autowired
    private SiteConst siteConst;

    /**
     * 读取用户SESSION信息
     * 
     * @param request
     * @return
     */
    public static User getLoginMember(HttpServletRequest request){
    	User loginMember = (User) request.getSession().getAttribute(Const.SESSION_USER);
        return loginMember;
    }

    /**
     * 写入用户SESSION信息
     * 
     * @param request
     * @param user
     */
    public void setLoginMember(HttpServletRequest request, HttpServletResponse response,boolean  keepLogin, User user){
        // 如果用户勾选保持登录，暂定过期时间为 3 年，否则为 120 分钟，单位为秒
        long liveSeconds =  keepLogin ? 3 * 365 * 24 * 60 * 60 : 120 * 60;
        // 传递给控制层的 cookie
        int maxAgeInSeconds = (int)(keepLogin ? liveSeconds : -1);
        // expireTime 用于设置 session 的过期时间点，需要转换成毫秒
        long expireTime = System.currentTimeMillis() + (liveSeconds * 1000);
        String sessionKey=Md5Utils.getMD5(String.valueOf(expireTime));
        HttpSession session=request.getSession(true);
        session.setMaxInactiveInterval(maxAgeInSeconds);
        user.setSessionKey(sessionKey);
        session.setAttribute(Const.SESSION_USER,user);

        Cookie cookie = new Cookie(siteConst.getSessionKey(),sessionKey);
        cookie.setPath("/");
        // 仅当配置了 cookieDomain 且与当前访问主机匹配时才设置 Domain 属性；
        // 未配置时不设置（浏览器默认限定为当前主机），避免把 cookie 发到不匹配的域被浏览器拒收
        String domain = request.getServerName();
        String configuredDomain = siteConst.getCookieDomain();
        if (configuredDomain != null && !configuredDomain.trim().isEmpty()
                && (domain.equals(configuredDomain) || domain.endsWith(configuredDomain))) {
            cookie.setDomain(configuredDomain);
        }
        cookie.setMaxAge(maxAgeInSeconds);
        response.addCookie(cookie);

        UserSession userSession=new UserSession();
        userSession.setSessionKey(sessionKey);
        userSession.setUserId(user.getUserId());
        userSession.setExpireTime(expireTime);
        userSession.setUpdateTime(new Date());
        if(userService.checkUserSessionByUserId(user.getUserId())){
            userService.updateUserSession(userSession);
        }else{
            userService.addUserSession(userSession);
        }
    }

    /**
     * 更新用户SESSION信息
     *
     * @param request
     * @param user
     */
    public void updateLoginMember(HttpServletRequest request, HttpServletResponse response,String sessionKey, User user){
        // 如果用户勾选保持登录，暂定过期时间为 3 年，否则为 120 分钟，单位为秒
        long liveSeconds =  3 * 365 * 24 * 60 * 60;
        // expireTime 用于设置 session 的过期时间点，需要转换成毫秒
        long expireTime = System.currentTimeMillis() + (liveSeconds * 1000);
        HttpSession session=request.getSession(true);
        session.setMaxInactiveInterval(-1);
        user.setSessionKey(sessionKey);
        session.setAttribute(Const.SESSION_USER,user);

        UserSession userSession=new UserSession();
        userSession.setSessionKey(sessionKey);
        userSession.setUserId(user.getUserId());
        userSession.setExpireTime(expireTime);
        userSession.setUpdateTime(new Date());
        if(userService.checkUserSessionByUserId(user.getUserId())){
            userService.updateUserSession(userSession);
        }
    }

    /**
     * 用户访问链接转跳判断
     * 
     * @param request
     * @param redirectUrl
     * @return
     */
    public static String judgeLoginJump(HttpServletRequest request,String redirectUrl){
    	User user = getLoginMember(request);
    	if(user == null){
            String redirect = "redirect:/ucenter/login";
            if(!StringUtils.isEmpty(redirectUrl)){
                redirect += "?redirectUrl="+request.getContextPath() + redirectUrl;
            }
            return redirect;
        }
        return null;
    }
}
