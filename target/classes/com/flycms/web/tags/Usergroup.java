package com.flycms.web.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.model.UserGroup;
import com.flycms.module.user.service.UserGroupService;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

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
public class Usergroup extends AbstractTagPlugin {
	@Autowired
	private UserGroupService userGroupService;
		
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		Long userId=0L;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){
			if("userId".equals(str)){
				if (!NumberUtils.isNumber(paramWrap.get(str).toString())) {
					userId = 0L;
				}else{
					userId = Long.parseLong(paramWrap.get(str).toString());
				}
			}
		}
		// 获取文章所有信息
		try {
			UserGroup group = userGroupService.findUuserGroupByUserId(userId);
			env.setVariable("group", builder.build().wrap(group));
			body.render(env.getOut());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
