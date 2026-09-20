package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.model.UserGroup;
import com.flycms.module.user.service.UserGroupService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sunkaifei
 * 
 */
@Service
public class Usergrouplist extends AbstractTagPlugin {

	@Autowired
	private UserGroupService userGroupService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		// 获取文件的分页
		try {
			List<UserGroup> list = userGroupService.getAllUserGroupList();
			env.setVariable("grouplist", builder.build().wrap(list));
		} catch (Exception e) {
			env.setVariable("grouplist", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
