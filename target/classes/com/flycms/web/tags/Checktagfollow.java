package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.topic.service.TopicService;
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
 * 版权：开源中国<br/>
 *
 * 查询是否已关注或者是该用户粉丝标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Checktagfollow extends AbstractTagPlugin {

	@Autowired
	protected TopicService topicService;

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {
			// 被关注者id
			Long userId = null;
			// 获取文件的分页
			//话题id
			Long topicId = null;
			//处理标签变量
			@SuppressWarnings("unchecked")
			Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
			for(String str:paramWrap.keySet()){ 
				if("userId".equals(str)){
					userId = Long.parseLong(paramWrap.get(str).toString());
				}
				if("topicId".equals(str)){
					topicId = Long.parseLong(paramWrap.get(str).toString());
				}

			}
			if(userId!=null && topicId!=null){
				boolean result=topicService.checkTopicByUserId(userId,topicId);
                env.setVariable("result", builder.build().wrap(result));
            }else {
				env.setVariable("result", builder.build().wrap(false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		body.render(env.getOut());
	}

}
