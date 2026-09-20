package com.flycms.web.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.admin.service.PermissionService;
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
 * 权限组权限查询查询标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Markrole extends AbstractTagPlugin {
	
	@Autowired
	private PermissionService srv;

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {
			// 获取页面的参数
			Long roleId = 0L;
			// 获取文件的分页
			//审核设置，默认0
			Long permissionId = 0L;
			//处理标签变量
			@SuppressWarnings("unchecked")
			Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
			for(String str:paramWrap.keySet()){ 
				if("roleId".equals(str)){
					roleId = Long.parseLong(paramWrap.get(str).toString());
				}
				if("permissionId".equals(str)){
					permissionId = Long.parseLong(paramWrap.get(str).toString());
				}

			}
			boolean mark = srv.markAssignedPermissions(roleId,permissionId);
			env.setVariable("mark", builder.build().wrap(mark));
			body.render(env.getOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
