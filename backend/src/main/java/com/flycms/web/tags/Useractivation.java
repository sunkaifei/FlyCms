package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.user.service.UserService;
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
 * 权限组权限查询查询标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Useractivation extends AbstractTagPlugin {
	
	@Autowired
	private UserService userService;
	@Autowired
	private ConfigService configService;

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {
			// 获取页面的参数
			Long userId=null;
			// 获取文件的分页
			//审核设置，默认0
			Long groupId = Long.parseLong(configService.getStringByKey("user_activation_role"));
			//处理标签变量
			@SuppressWarnings("unchecked")
			Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
			for(String str:paramWrap.keySet()){ 
				if("userId".equals(str)){
					userId = Long.parseLong(paramWrap.get(str).toString());
				}
			}
			boolean status = userService.checkUserByActivation(userId,groupId);
			env.setVariable("status", builder.build().wrap(status));
			body.render(env.getOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
