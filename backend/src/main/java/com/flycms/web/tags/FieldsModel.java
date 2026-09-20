package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelFieldService;
import com.flycms.module.model.service.ModelService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义模型字段元数据标签（详情页动态展示用）。params: model(code)；输出变量 fieldsList。
 */
@Service
public class FieldsModel extends AbstractTagPlugin {

	@Autowired
	private ModelFieldService modelFieldService;
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
		env.setVariable("fieldsList", builder.build().wrap(
				model == null ? null : modelFieldService.findFieldsByModelId(model.getId(), 1)));
		body.render(env.getOut());
	}
}
