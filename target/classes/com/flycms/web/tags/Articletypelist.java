package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.utils.StringHelperUtils;
import com.flycms.module.article.model.ArticleCategory;
import com.flycms.module.article.service.ArticleCategoryService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.apache.commons.lang.math.NumberUtils;
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
public class Articletypelist extends AbstractTagPlugin {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//指定父级id
		long fatherId = 0;
		
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("fatherId".equals(str)){
				if(paramWrap.get(str).toString()==null) {
					fatherId=0;
				}else {
					if(!StringHelperUtils.checkInteger(paramWrap.get(str).toString())) {
						fatherId=0;
					}else {
						fatherId = Long.parseLong(paramWrap.get(str).toString());
					}
				}
			}
		}
		// 获取文件的分页
		try {
			List<ArticleCategory> pageVo = articleCategoryService.getCategoryListByFatherId(fatherId);
			env.setVariable("typelist", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("typelist", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
