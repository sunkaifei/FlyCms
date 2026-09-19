package com.flycms.web.tags;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.utils.DateUtils;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * 时间标签
 * 
 * @author sunaifei
 */
@Service
public class Dateformat extends AbstractTagPlugin {

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		// 获取页面的参数
		String time = params.get("time").toString();
		try {
			time = DateUtils.getDateTimeString(time);
			env.getOut().write(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
