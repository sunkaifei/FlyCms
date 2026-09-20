package com.flycms.web.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sunkaifei
 * 
 */
@Service
public class Questionpage extends AbstractTagPlugin {

	@Autowired
	private QuestionService questionService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//所属主信息类型，0是所有，1是文章，2是小组话题
		String title = null;

		Long userId = null;

		String createTime=null;
		
		Integer status=null;
		/**
		 *
		 * § orderby='hot' 或 orderby='click' 表示按点击数排列
		 * § orderby='sortrank' 或 orderby='pubdate' 按出版时间排列
		 * § orderby=='lastpost' 按最后评论时间
		 * § orderby=='scores' 按得分排序
		 * § orderby='id' 按文章ID排序
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
			if("title".equals(str)){
				title = paramWrap.get(str).toString();
			}
			if("userId".equals(str)){
				userId = Long.parseLong(paramWrap.get(str).toString());
			}
			if("createTime".equals(str)){
				createTime = paramWrap.get(str).toString();
			}
            if("orderby".equals(str)){
                orderby = paramWrap.get(str).toString();
            }
            if("order".equals(str)){
                order = paramWrap.get(str).toString();
            }
			if("status".equals(str)){
				status = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("p".equals(str)){
				p = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("rows".equals(str)){
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		try {
			PageVo<Question> pageVo = questionService.getQuestionListPage(title,userId,createTime,status,orderby,order,p,rows);
			env.setVariable("question_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("question_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
