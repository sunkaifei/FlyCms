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
 *  查询粉丝或者所关注标签
 *
 * @author sunkaifei
 * 
 */
@Service
public class Userfanspage extends AbstractTagPlugin {

	@Autowired
	private UserService userService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//用户名
		Long userFollow = null;
		//昵称
		Long userFans = null;
		//手机号码
		String time = null;

        String orderby=null;

        String order=null;
		//当前页数
		int p = 1;
		//每页记录数
		int rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){
			if("userFollow".equals(str)){
				userFollow = Long.parseLong(paramWrap.get(str).toString());
			}
			if("userFans".equals(str)){
				userFans = Long.parseLong(paramWrap.get(str).toString());
			}
			if("time".equals(str)){
				time = paramWrap.get(str).toString();
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
			PageVo<UserFans> pageVo = userService.getUserFansListPage(userFollow,userFans,time,orderby,order,p,rows);
			env.setVariable("fans_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("fans_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
