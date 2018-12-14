package com.flycms.web.tags;

import static freemarker.template.ObjectWrapper.BEANS_WRAPPER;

import java.io.IOException;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import freemarker.template.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;

/**
 * 字符串截取标签
 *
 * @author Administrator
 */
@Service
public class Stringcut extends AbstractTagPlugin {


	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_28);
		// 获取页面的参数
		String content = params.get("content").toString();
		Integer num = Integer.parseInt(params.get("num").toString());
		content = Jsoup.clean(content, Whitelist.none());
		content = StringUtils.abbreviate(content, num);
		env.setVariable("info_content", builder.build().wrap(content));
		body.render(env.getOut());
	}
}
