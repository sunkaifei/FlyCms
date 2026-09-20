package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.service.ModelCategoryService;
import com.flycms.module.model.service.ModelService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义模型分类列表标签（对应 DedeCMS channel/type）。
 * 模板用法：
 * <@fly_category_model model="downloads">
 *   <#list categoryList as c>${c.name}</#list>
 * </@fly_category_model>
 * params: model(code)；输出变量 categoryList
 */
@Service
public class CategoryModel extends AbstractTagPlugin {

	@Autowired
	private ModelCategoryService modelCategoryService;
	@Autowired
	private ModelService modelService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		String modelCode = null;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("model".equals(str)) {
				modelCode = paramWrap.get(str).toString();
			}
		}
		Model model = modelService.findModelByCode(modelCode);
		java.util.List<ModelCategory> categoryList = model == null
				? null
				: modelCategoryService.findCategoriesByModelId(model.getId(), 1);
		env.setVariable("categoryList", builder.build().wrap(categoryList));
		body.render(env.getOut());
	}
}
