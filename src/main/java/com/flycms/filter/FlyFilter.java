package com.flycms.filter;

import com.flycms.constant.Const;
import com.flycms.constant.SiteConst;
import com.flycms.core.base.BaseController;
import com.flycms.core.utils.CookieUtils;
import com.flycms.module.user.model.User;
import com.flycms.module.user.model.UserSession;
import com.flycms.module.user.service.UserService;
import com.flycms.module.user.utils.UserSessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebFilter;
import javax.servlet.*;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 全站过滤器，获取用户url携带邀请参数，记录邀请人id
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 9:51 2018/9/12
 */
@Slf4j
@WebFilter(filterName="myFilter",urlPatterns="/*")
public class FlyFilter implements Filter {
    @Autowired
    private UserService userService;

    @Autowired
    private UserSessionUtils userSessionUtils;
    @Autowired
    private SiteConst siteConst;

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //System.out.println("MyFilter init............");
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        //用户被邀请uid创建cookie记录
        String invite=request.getParameter("invite");
        if(!StringUtils.isBlank(invite)){
            CookieUtils.writeCookie(httpResponse,"invite",invite,60*60*24*7);
        }
        String sessionKey=CookieUtils.getCookie(httpRequest,siteConst.getSessionKey());
        if(sessionKey!=null){
            if(session!=null && session.getAttribute(Const.SESSION_USER) != null){
                User userLogin = (User) httpRequest.getSession().getAttribute(Const.SESSION_USER);
                if(!(userLogin.getSessionKey()).equals(sessionKey)){
                    UserSession userSession=userService.findUserSessionBySeeeionKey(sessionKey);
                    if(userSession!=null){
                        if (!userService.isExpireTime(userSession.getExpireTime())) {  // session 未过期
                            User user=userService.findUserById(userSession.getUserId(),0);
                            long expireTime = System.currentTimeMillis() + (120 * 60 * 1000);
                            boolean keepLogin = userSession.getExpireTime()> expireTime ? true : false;
                            //用户信息写入session
                            userSessionUtils.setLoginMember(httpRequest,httpResponse,keepLogin,user);
                        }else{
                            //过期得话注销cookie、session和登录保持记录
                            userService.signOutLogin(httpRequest,httpResponse);
                        }
                    }
                }
            }else{
                UserSession userSession=userService.findUserSessionBySeeeionKey(sessionKey);
                if(userSession!=null){
                    if (!userService.isExpireTime(userSession.getExpireTime())) {  // session 未过期
                        User user=userService.findUserById(userSession.getUserId(),0);
                        //用户信息更新session
                        userSessionUtils.updateLoginMember(httpRequest,httpResponse,sessionKey,user);
                    }else{
                        //过期得话注销cookie、session和登录保持记录
                        userService.signOutLogin(httpRequest,httpResponse);
                    }
                }
            }
        }else{
            //过期得话注销cookie、session和登录保持记录
            userService.signOutLogin(httpRequest,httpResponse);
        }
        //System.out.println("MyFilter doFilter.........before");
        chain.doFilter(request, response);
        //System.out.println("MyFilter doFilter.........after");
    }

    @Override
    public void destroy() {
        //System.out.println("MyFilter destroy..........");
    }
}