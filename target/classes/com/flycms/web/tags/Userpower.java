package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserGroupService;
import com.flycms.module.user.utils.UserSessionUtils;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 查询用户组标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Userpower extends AbstractTagPlugin {
	@Autowired
	private UserGroupService userGroupService;
		
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);

		User user = UserSessionUtils.getLoginMember(request);
		// 获取页面的参数
		String groupName=null;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){
			if("groupName".equals(str)){
				groupName = paramWrap.get(str).toString();
			}
		}
		// 获取用户所在的权限组
		try {
			boolean result = userGroupService.checkUuserPower(groupName,user.getUserId());
			if(result){
				body.render(env.getOut());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
