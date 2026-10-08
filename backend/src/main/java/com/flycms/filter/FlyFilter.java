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

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.*;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
    private com.flycms.module.redirect.service.RedirectService redirectService;

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

        // T-b SEO 重定向：命中规则的请求 301 到目标地址（缓存全表，未命中走原流程）
        try {
            String target = redirectService.match(httpRequest.getRequestURI());
            if (target != null) {
                httpResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                httpResponse.setHeader("Location", target);
                return;
            }
        } catch (Exception e) {
            // 重定向查询失败不阻塞请求
        }

        //用户被邀请uid创建cookie记录
        // 只接受纯数字邀请人 id：该值来自 URL 参数且会被写入 Set-Cookie，
        // 不做白名单则属于「用户可控内容进响应头」，可造成 Cookie 注入/500
        String invite=request.getParameter("invite");
        if(!StringUtils.isBlank(invite) && invite.matches("\\d{1,19}")){
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
                            // 用户可能已被删除/账号记录缺失（inner join 为空），跳过自动登录而非 NPE
                            if(user != null){
                                long expireTime = System.currentTimeMillis() + (120 * 60 * 1000);
                                boolean keepLogin = userSession.getExpireTime()> expireTime ? true : false;
                                //用户信息写入session
                                userSessionUtils.setLoginMember(httpRequest,httpResponse,keepLogin,user);
                            }
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
                        // 用户可能已被删除/账号记录缺失（inner join 为空），跳过自动登录而非 NPE
                        if(user != null){
                            //用户信息更新session
                            userSessionUtils.updateLoginMember(httpRequest,httpResponse,sessionKey,user);
                        }
                    }else{
                        //过期得话注销cookie、session和登录保持记录
                        userService.signOutLogin(httpRequest,httpResponse);
                    }
                }
            }
        }else if(session != null){
            // 无会话 cookie 且「当前确实存在会话」才需要注销；
            // 原实现在此分支无条件调用 signOutLogin，而它内部用 request.getSession()
            // 会为每个匿名请求（含全部静态资源）新建 session —— 本站最大的固定开销来源。
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