package com.flycms.web.tags;

import java.io.IOException;
import java.util.Map;

import com.flycms.core.utils.StringHelperUtils;
import com.flycms.core.base.AbstractTagPlugin;
import freemarker.template.*;
import org.springframework.stereotype.Service;

import freemarker.core.Environment;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author Administrator 头像标签
 */
@Service
public class Avatar extends AbstractTagPlugin {


	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		// 获取页面的参数
		String avatarurl = params.get("avatarurl").toString();
		String avatar = params.get("avatar").toString();
		avatar = StringHelperUtils.TextReplace(avatarurl,avatar);
		env.setVariable("avatar", builder.build().wrap(avatar));
		env.getOut().write(avatar);
	}
}
