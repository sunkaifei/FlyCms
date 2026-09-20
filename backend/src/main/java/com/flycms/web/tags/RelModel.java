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
 * 相关内容标签（对应 DedeCMS likearticle）：同模型同分类、排除指定 id。
 * 模板用法：
 * <@fly_rel_model model="downloads" category="${info.categoryId}" notid="${info.id}" rows="5">
 *   <#list dataList as item>...</#list>
 * </@fly_rel_model>
 * params: model(code), category(分类id，可省), notid(排除的内容id，详情页传自身), rows；输出变量 dataList
 */
@Service
public class RelModel extends AbstractTagPlugin {

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
		Long notId = null;
		int rows = 5;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("model".equals(str)) {
				modelCode = paramWrap.get(str).toString();
			}
			if ("category".equals(str)) {
				categoryId = Long.parseLong(paramWrap.get(str).toString());
			}
			if ("notid".equals(str)) {
				notId = Long.parseLong(paramWrap.get(str).toString());
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
						.selectPage(model.getId(), null, categoryId, 1, null, null, null, 1, rows, notId, true)
						.getList();
				modelDataService.expandAttachments(model.getId(), dataList);
			} catch (Exception ignored) {
			}
		}
		env.setVariable("dataList", builder.build().wrap(dataList));
		body.render(env.getOut());
	}
}
