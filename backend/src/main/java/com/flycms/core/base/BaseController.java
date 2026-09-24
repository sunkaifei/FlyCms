package com.flycms.core.base;

import com.flycms.module.user.utils.UserSessionUtils;
import com.flycms.module.template.service.TemplateService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
	// 旧后台的 getAdminUser() 已随旧后台下线移除；API 管理员会话统一走 ApiBaseController.requireAdmin()
}
