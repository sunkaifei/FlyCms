package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.topic.model.TopicInfo;
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
 * @author sunkaifei
 * 
 */
@Service
public class Topicinfopage extends AbstractTagPlugin {

	@Autowired
	private TopicService topicService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//所属主信息类型，0是所有，1是文章，2是小组话题
		Integer infoType = null;

		Long topicId = null;

		Integer status=2;
		/**
		 *
		 * § orderby='hot' 或 orderby='click' 表示按点击数排列
		 * § orderby='sortrank' 或 orderby='pubdate' 按添加时间排列
		 * § orderby=='scores' 按得分排序 (未实现)
		 * § orderby='id' 按文章ID排序
		 * § orderby='rand' 随机获得指定条件的列表
		 */
		String orderby=null;

		String order=null;

		//翻页页数
		Integer p = 1;
		//每页记录条数
		Integer rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("infoType".equals(str)){
				infoType = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("topicId".equals(str)){
				topicId = Long.parseLong(paramWrap.get(str).toString());
			}
			if("status".equals(str)){
				status = Integer.parseInt(paramWrap.get(str).toString());
			}
            if("orderby".equals(str)){
                orderby = paramWrap.get(str).toString();
            }

            if("order".equals(str)){
                order = paramWrap.get(str).toString();
            }
			if("p".equals(str)){
				p = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("rows".equals(str)){
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		System.out.println("===================1==============");
		try {
			System.out.println("===================2==============");
			PageVo<TopicInfo> pageVo = topicService.getTopicAndInfoListPage(infoType,topicId,status,orderby,order,p,rows);
			System.out.println("===================3==============");
			env.setVariable("topic_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			System.out.println("===================4==============");
			env.setVariable("topic_page", builder.build().wrap(null));
		}
		System.out.println("===================5==============");
		body.render(env.getOut());
	}
}
