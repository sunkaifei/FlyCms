package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
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
 * 热门内容标签（对应 DedeCMS arclist orderby=hot）：按浏览量倒序。
 * 模板用法：
 * <@fly_hot_model model="downloads" rows="10">
 *   <#list dataList as item>${item.title}（${item.countView}）</#list>
 * </@fly_hot_model>
 * params: model(code), rows(默认10), category(可选)；输出变量 dataList
 */
@Service
public class HotModel extends AbstractTagPlugin {

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
		int rows = 10;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("model".equals(str)) {
				modelCode = paramWrap.get(str).toString();
			}
			if ("category".equals(str)) {
				categoryId = Long.parseLong(paramWrap.get(str).toString());
			}
			if ("rows".equals(str)) {
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		Model model = modelService.findModelByCode(modelCode);
		java.util.List<Map<String, Object>> dataList = null;
		if (model != null) {
			try {
				dataList = modelDataService
						.selectPage(model.getId(), null, categoryId, 1, null, "count_view", "desc", 1, rows, null, true)
						.getList();
				modelDataService.expandAttachments(model.getId(), dataList);
			} catch (Exception ignored) {
			}
		}
		env.setVariable("dataList", builder.build().wrap(dataList));
		body.render(env.getOut());
	}
}
