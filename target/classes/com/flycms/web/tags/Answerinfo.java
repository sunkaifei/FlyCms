package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.question.model.Answer;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.service.AnswerService;
import com.flycms.module.question.service.QuestionService;
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
 * 版权：开源中国<br/>*
 *
 * 用户信息查询标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Answerinfo extends AbstractTagPlugin {

	@Autowired
	private AnswerService answerService;

	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		Long answerId = null;
		Integer status = 0;
		
		@SuppressWarnings("unchecked")
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("answerId".equals(str)){
				answerId = Long.parseLong(paramWrap.get(str).toString());
			}
			if("status".equals(str)){
				status = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		Answer answer = answerService.findAnswerById(answerId,status);
		env.setVariable("answer", builder.build().wrap(answer));
		body.render(env.getOut());
	}

}
