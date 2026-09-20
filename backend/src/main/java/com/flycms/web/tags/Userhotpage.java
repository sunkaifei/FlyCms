package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.user.model.User;
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
 * @author sunkaifei
 * 
 */
@Service
public class Userhotpage extends AbstractTagPlugin {

	@Autowired
	private UserService userService;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		//用户名
		String userName = null;
		//昵称
		String nickName = null;
		//手机号码
		String mobile = null;
		//邮箱
		String email = null;
		//省份id
		Integer province = null;
		//地区id
		Integer city = null;
		//县市id
		Integer area = null;
		//审核状态
		Integer status = null;

        String orderby=null;

        String order=null;
		//当前页数
		int p = 1;
		//每页记录数
		int rows = 10;
		//处理标签变量
		Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
		for(String str:paramWrap.keySet()){
			if("userName".equals(str)){
				userName = paramWrap.get(str).toString();
			}
			if("nickName".equals(str)){
				nickName = paramWrap.get(str).toString();
			}
			if("mobile".equals(str)){
				mobile = paramWrap.get(str).toString();
			}
			if("email".equals(str)){
				email = paramWrap.get(str).toString();
			}
			if("province".equals(str)){
				province = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("city".equals(str)){
				city = Integer.parseInt(paramWrap.get(str).toString());
			}
			if("area".equals(str)){
				area = Integer.parseInt(paramWrap.get(str).toString());
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
		try {
			PageVo<User> pageVo = userService.getUserHotListPage(userName, nickName, mobile, email,province,city,area,status,orderby,order,p,rows);
			env.setVariable("hot_page", builder.build().wrap(pageVo));
		} catch (Exception e) {
			env.setVariable("hot_page", builder.build().wrap(null));
		}
		body.render(env.getOut());
	}
}
