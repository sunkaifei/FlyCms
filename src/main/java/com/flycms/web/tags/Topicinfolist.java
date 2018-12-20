package com.flycms.web.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.service.TopicService;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;

/**
 * 文章内容页标签列表标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Topicinfolist extends AbstractTagPlugin {

	@Autowired
	private TopicService topicService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_28);
		// 获取页面的参数
		//
		long infoId = 0;
		Integer type = 0;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("type".equals(str)){
				type = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("infoId".equals(str)){
				infoId = Long.parseLong(paramWrap.get(str).toString());
			}
		}
		List<Topic> topiclist = topicService.getInfoByTopicList(type,infoId);
		env.setVariable("topiclist", builder.build().wrap(topiclist));
		body.render(env.getOut());
	}

}
