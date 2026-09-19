package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.admin.model.Group;
import com.flycms.module.admin.service.GroupService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.apache.commons.lang.math.NumberUtils;
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
 * 查询管理员所在权限组标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Adminrole extends AbstractTagPlugin {
	@Autowired
	protected GroupService srv;
		
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		Long id=0L;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){
			if("id".equals(str)){
				if (!NumberUtils.isNumber(paramWrap.get(str).toString())) {
					id = 0L;
				}else{
					id = Long.parseLong(paramWrap.get(str).toString());
				}
			}
		}
		// 按管理员id查询所在会员组信息
		try {
			Group group = srv.findUserByGroup(id);
			if(group!=null){
				env.getOut().write(group.getName());
			}else{
				env.getOut().write("");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
