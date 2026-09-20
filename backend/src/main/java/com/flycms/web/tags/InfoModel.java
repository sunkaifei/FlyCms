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
 * 自定义模型详情标签。params: model(code) + id 或 shortUrl；输出变量 info。
 */
@Service
public class InfoModel extends AbstractTagPlugin {

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
		Long id = null;
		String shortUrl = null;
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for (String str : paramWrap.keySet()) {
			if ("model".equals(str)) {
				modelCode = paramWrap.get(str).toString();
			}
			if ("id".equals(str)) {
				id = Long.parseLong(paramWrap.get(str).toString());
			}
			if ("shortUrl".equals(str)) {
				shortUrl = paramWrap.get(str).toString();
			}
		}
		Model model = modelService.findModelByCode(modelCode);
		Map<String, Object> info = null;
		if (model != null) {
			if (id != null) {
				info = modelDataService.findDataById(model.getId(), id);
			} else if (shortUrl != null) {
				info = modelDataService.findByShortUrl(model.getId(), shortUrl);
			}
		}
		env.setVariable("info", builder.build().wrap(info));
		body.render(env.getOut());
	}
}
