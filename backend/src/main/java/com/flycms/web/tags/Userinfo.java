package com.flycms.web.tags;

import static freemarker.template.ObjectWrapper.BEANS_WRAPPER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.model.User;
import com.flycms.module.user.model.UserCount;
import com.flycms.module.user.service.UserService;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>*
 *
 * 用户信息查询标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Userinfo extends AbstractTagPlugin {
	
	@Autowired
	protected UserService userService;

	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		Long userId = null;
		Integer status = 0;
		
		@SuppressWarnings("unchecked")
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("userId".equals(str)){
                userId = Long.parseLong(paramWrap.get(str).toString());
			}
			if("status".equals(str)){
				status = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		User userinfo = userService.findUserById(userId,status);
		if(userinfo!=null){
			UserCount count=userService.findUserCountById(userinfo.getUserId());
			userinfo.setCountFans(count.getCountFans());
			userinfo.setCountAnswer(count.getCountAnswer());
			userinfo.setCountArticle(count.getCountArticle());
			userinfo.setCountQuestion(count.getCountQuestion());
		}
		env.setVariable("userinfo", builder.build().wrap(userinfo));
		body.render(env.getOut());
	}

}
