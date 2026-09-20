package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.service.UserPermissionService;
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
public class Markgroup extends AbstractTagPlugin {

	@Autowired
	protected UserPermissionService userPermissionService;

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {
			// 获取页面的参数
			Long groupId = 0L;
			// 获取文件的分页
			//审核设置，默认0
			Long permissionId = 0L;
			//处理标签变量
			@SuppressWarnings("unchecked")
			Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
			for(String str:paramWrap.keySet()){ 
				if("groupId".equals(str)){
					groupId = Long.parseLong(paramWrap.get(str).toString());
				}
				if("permissionId".equals(str)){
					permissionId = Long.parseLong(paramWrap.get(str).toString());
				}

			}
			boolean mark = userPermissionService.markAssignedPermissions(groupId,permissionId);
			env.setVariable("mark", builder.build().wrap(mark));
			body.render(env.getOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
