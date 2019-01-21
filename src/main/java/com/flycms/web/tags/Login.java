package com.flycms.web.tags;

import com.flycms.constant.Const;
import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.user.model.User;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 网站样式等静态资源路径查询标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Login extends AbstractTagPlugin {

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {

			//获取HttpSession
			User loginMember = (User) request.getSession().getAttribute(Const.SESSION_USER);
			env.setVariable("status", builder.build().wrap(loginMember));
			body.render(env.getOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
