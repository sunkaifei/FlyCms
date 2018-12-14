package com.flycms.core.utils;

import com.flycms.constant.Const;
import com.flycms.module.admin.model.Admin;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

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
 * @email admin@28844.com
 * @version 1.0 <br/>
 * 
 */
public class AdminSessionUtils {

    /**
     * 读取用户SESSION信息
     * 
     * @param request
     * @return
     */
    public static Admin getLoginMember(HttpServletRequest request){
        Admin loginMember = (Admin) request.getSession().getAttribute(Const.SESSION_ADMIN);
        return loginMember;
    }

    /**
     * 写入用户SESSION信息
     * 
     * @param request
     * @param admin
     */
    public static void setLoginMember(HttpServletRequest request,Admin admin){
        request.getSession().setAttribute(Const.SESSION_ADMIN,admin);
    }

    /**
     * 用户访问链接转跳判断
     * 
     * @param request
     * @param redirectUrl
     * @return
     */
    public static String judgeLoginJump(HttpServletRequest request,String redirectUrl){
        Admin user = getLoginMember(request);
    	if(user == null){
            String redirect = "redirect:/admin/login";
            if(!StringUtils.isEmpty(redirectUrl)){
                redirect += "?redirectUrl="+request.getContextPath() + redirectUrl;
            }
            return redirect;
        }
        return null;
    }
}
