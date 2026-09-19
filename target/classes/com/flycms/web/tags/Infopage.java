package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.StringHelperUtils;
import com.flycms.module.question.model.Answer;
import com.flycms.module.search.model.Info;
import com.flycms.module.search.service.SolrService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.apache.commons.lang3.StringUtils;
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
public class Infopage extends AbstractTagPlugin {

	@Autowired
	private SolrService solrService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//问题id
		String title = null;
		//用户id
		Long userId = null;
		//信息类型，0是全部，1问答，2文章，3分享
		Integer infoType = null;
		//按信息分类id查询
		Long categoryId = null;
		//需要排除id
		String notId=null;
		//排序规则,recommend按推荐值排序，weight按权重值排序
		String orderby=null;
		//当前页数
		int p = 1;
		//每页记录数
		int rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("title".equals(str)){
				title = paramWrap.get(str).toString();
			}
			if("userId".equals(str)){
				userId = Long.parseLong(paramWrap.get(str).toString());
			}
			if("infoType".equals(str)){
				if(!StringUtils.isBlank(paramWrap.get(str).toString())) {
					if(!StringHelperUtils.checkInteger(paramWrap.get(str).toString())) {
						infoType=0;
					}else{
						infoType = Integer.parseInt(paramWrap.get(str).toString());
					}
				}else{
					infoType=0;
				}

			}
			if("categoryId".equals(str)){
				if(!StringUtils.isBlank(paramWrap.get(str).toString())){
					categoryId = Long.parseLong(paramWrap.get(str).toString());
				}else{
					categoryId = null;
				}
			}
			if("notId".equals(str)){
				notId = paramWrap.get(str).toString();
			}
			if("orderby".equals(str)){
				orderby = paramWrap.get(str).toString();
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
			PageVo<Info> pageVo = solrService.searchInfo(title,userId,infoType,categoryId,notId,orderby,p,rows);
			env.setVariable("info_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("info_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
