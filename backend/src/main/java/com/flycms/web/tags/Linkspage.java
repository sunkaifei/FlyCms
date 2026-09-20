package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.links.model.Links;
import com.flycms.module.links.service.LinksService;
import com.flycms.module.question.model.Answer;
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
 * @author sunkaifei
 * 
 */
@Service
public class Linkspage extends AbstractTagPlugin {

	@Autowired
	private LinksService linksService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//问题id
		Integer type = 0;
		//用户id
		Integer show = 0;
		//当前页数
		int p = 1;
		//每页记录数
		int rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("type".equals(str)){
				type = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("show".equals(str)){
				show = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("p".equals(str)){
				p = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("rows".equals(str)){
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		try {
			PageVo<Links> pageVo = linksService.getLinksListPage(type,show,p,rows);
			env.setVariable("link_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("link_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
