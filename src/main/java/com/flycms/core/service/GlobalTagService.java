package com.flycms.core.service;

import com.flycms.module.config.service.ConfigService;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class GlobalTagService{
	@Autowired
	private Configuration configuration;

    @Autowired
    private ConfigService configService;

	@PostConstruct
	public void setSharedVariable() throws TemplateModelException {
		//网站名称
	    configuration.setSharedVariable("web_name", configService.getStringByKey("fly_title"));
	    //网页主页链接
        configuration.setSharedVariable("web_url", configService.getStringByKey("fly_url"));
        //网页主页链接
        configuration.setSharedVariable("web_logo", configService.getStringByKey("fly_logo"));
        //首页标题
        configuration.setSharedVariable("seo_title", configService.getStringByKey("fly_seo_title"));
        //网站关键字
        configuration.setSharedVariable("seo_keywords", configService.getStringByKey("fly_seo_keywords"));
        //网站描述
        configuration.setSharedVariable("seo_description", configService.getStringByKey("fly_seo_description"));
        //站点底部信息
        configuration.setSharedVariable("web_footer_code", configService.getStringByKey("fly_footer_code"));
        //备案信息
        configuration.setSharedVariable("web_beian", configService.getStringByKey("fly_beian"));
        //pc模板路径
        configuration.setSharedVariable("pc_templets_skin", configService.getStringByKey("pc_theme"));
        //移动端模板
        configuration.setSharedVariable("pc_templets_skin", configService.getStringByKey("m_theme"));
	}

}
