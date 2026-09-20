package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义模型列表标签（手册 §7.3）。模板用法见模型默认模板骨架。
 */
@Service
public class ListModel extends AbstractTagPlugin {

	@Autowired
	private ModelDataService modelDataService;
	@Autowired
	private ModelService modelService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		String modelCode = null;
		Long categoryId = null;
		String title = null;
		String orderby = null;
		String order = null;
		int p = 1;
		int rows = 10;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("model".equals(str)) {
				modelCode = paramWrap.get(str).toString();
			}
			if ("category".equals(str)) {
				categoryId = Long.parseLong(paramWrap.get(str).toString());
			}
			if ("title".equals(str)) {
				title = paramWrap.get(str).toString();
			}
			if ("orderby".equals(str)) {
				orderby = paramWrap.get(str).toString();
			}
			if ("order".equals(str)) {
				order = paramWrap.get(str).toString();
			}
			if ("p".equals(str)) {
				p = Integer.parseInt(paramWrap.get(str).toString());
			}
			if ("rows".equals(str)) {
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		Model model = modelService.findModelByCode(modelCode);
		Map<String, String> filters = new HashMap<String, String>();
		for (String str : paramWrap.keySet()) {
			if (!"model".equals(str) && !"category".equals(str) && !"title".equals(str)
					&& !"orderby".equals(str) && !"order".equals(str) && !"p".equals(str) && !"rows".equals(str)) {
				filters.put(str, paramWrap.get(str).toString());
			}
		}
		if (model == null) {
			env.setVariable("dataList", builder.build().wrap(null));
			env.setVariable("model_page", builder.build().wrap(null));
			body.render(env.getOut());
			return;
		}
		try {
			PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
					model.getId(), title, categoryId, 1, filters, orderby, order, p, rows, true);
			env.setVariable("dataList", builder.build().wrap(pageVo.getList()));
			env.setVariable("model_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("dataList", builder.build().wrap(null));
			env.setVariable("model_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
