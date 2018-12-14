package com.flycms.core.base;

import com.flycms.core.service.GlobalTagService;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 *  网站基础标签处理类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:14 2018/7/8
 */

@Service
public abstract class GlobalTag extends ApplicationObjectSupport implements TemplateDirectiveModel, Plugin {

    @Autowired
    protected GlobalTagService globalTagService;

	@PostConstruct
	public void setSharedVariable() throws TemplateModelException {
		globalTagService.setSharedVariable();
	}

}
