package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.user.model.UserFans;
import com.flycms.module.user.service.UserService;
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
 * 粉丝列表标签
 *
 * @author sunkaifei
 * 
 */
@Service
public class Fanspage extends AbstractTagPlugin {

	@Autowired
	private UserService userService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//被关注者用户id
		Long userFollow = null;
		//粉丝id，关注者的id
		Long userFans = null;

		String createTime=null;

		String orderby=null;

		String order=null;

		//翻页页数
		Integer p = 1;
		//每页记录条数
		Integer rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){ 
			if("userFollow".equals(str)){
				userFollow = Long.parseLong(paramWrap.get(str).toString());
			}
			if("userFans".equals(str)){
				userFans = Long.parseLong(paramWrap.get(str).toString());
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
			if("p".equals(str)){
				p = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("rows".equals(str)){
				rows = Integer.parseInt(paramWrap.get(str).toString());
			}
		}
		// 获取文件的分页
		try {
			PageVo<UserFans> pageVo = userService.getUserFansListPage(userFollow,userFans,createTime,orderby,order,p,rows);
			env.setVariable("fans_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("fans_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
