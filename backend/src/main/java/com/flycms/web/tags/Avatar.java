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
		// 缺参降级为空串，不再 NPE（P4-4：原实现 params.get(...).toString() 在缺参时直接抛 Cannot invoke）
		Object rawUrl = params.get("avatarurl");
		Object rawAvatar = params.get("avatar");
		String avatarurl = rawUrl == null ? "" : rawUrl.toString();
		String avatar = rawAvatar == null ? "" : rawAvatar.toString();
		avatar = StringHelperUtils.TextReplace(avatarurl,avatar);
		env.setVariable("avatar", builder.build().wrap(avatar));
		env.getOut().write(avatar);
	}
}
