package com.flycms.web.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flycms.core.utils.StringHelperUtils;
import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.config.model.Areas;
import com.flycms.module.config.service.AreasService;
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
 * @author sunkaifei
 * 
 */
@Service
public class Areaslist extends AbstractTagPlugin {

	@Autowired
	private AreasService areasService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//指定父级id
		int parentId = 0;
		
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("parentId".equals(str)){
				if(!NumberUtils.isNumber(paramWrap.get(str).toString())) {
					parentId=0;
				}else {
					if(!StringHelperUtils.checkInteger(paramWrap.get(str).toString())) {
						parentId=0;
					}else {
						parentId = Integer.parseInt(paramWrap.get(str).toString());
					}
				}
			}
		}
		// 获取文件的分页
		try {
			List<Areas> pageVo = areasService.selectAreasByPid(parentId);
			env.setVariable("areaslist", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("areaslist", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
