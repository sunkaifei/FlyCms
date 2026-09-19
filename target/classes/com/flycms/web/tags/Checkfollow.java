package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
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
 * 查询是否已关注或者是该用户粉丝标签
 * 
 * @author sunkaifei
 * 
 */
@Service
public class Checkfollow extends AbstractTagPlugin {

	@Autowired
	protected UserService userService;

	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		try {
			// 被关注者id
			Long userFollow = null;
			// 获取文件的分页
			//粉丝id
			Long userFans = null;
			//关注状态，0未关注，1已关注，2已互相关注
            Integer status = 0;
			//处理标签变量
			@SuppressWarnings("unchecked")
			Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
			for(String str:paramWrap.keySet()){ 
				if("userFollow".equals(str)){
					userFollow = Long.parseLong(paramWrap.get(str).toString());
				}
				if("userFans".equals(str)){
					userFans = Long.parseLong(paramWrap.get(str).toString());
				}

			}
			if(userFollow==null && userFans==null){
                status = 0;
				env.setVariable("result", builder.build().wrap(status));
			}else if(userService.checkUserFans(userFollow,userFans)){
                status = 2;
                env.setVariable("result", builder.build().wrap(status));
			}else if(userService.checkUserFans(userFollow,userFans)){
                status = 1;
				env.setVariable("result", builder.build().wrap(status));
			}else{
                status = 0;
                env.setVariable("result", builder.build().wrap(status));
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
		body.render(env.getOut());
	}

}
