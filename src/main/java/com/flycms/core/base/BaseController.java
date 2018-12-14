package com.flycms.core.base;

import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.module.user.utils.UserSessionUtils;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.service.AdminService;
import com.flycms.module.template.service.TemplateService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 *  Controller基类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */

public class BaseController {
    @Resource
    protected HttpServletRequest request;
    @Resource
    protected HttpServletResponse response;
	@Autowired
	protected AdminService adminService;
    @Autowired
	protected UserService userService;
    @Resource
    protected HttpSession session;
	@Autowired
	protected TemplateService theme;

	  /**
	   * 获取用户信息
	   *
	   * @return
	   */
	protected User getUser() {
		User user = UserSessionUtils.getLoginMember(request);
		if (StringUtils.isEmpty(user)) {
			return null;
		} else {
			return userService.findUserById(user.getUserId(),0);
		}
	}

	/**
	 * 获取用户信息
	 *
	 * @return
	 */
	protected Admin getAdminUser() {
		Admin user = AdminSessionUtils.getLoginMember(request);
		if (StringUtils.isEmpty(user)) {
			return null;
		} else {
			return adminService.findAdminById(user.getId(),0);
		}
	}
}
