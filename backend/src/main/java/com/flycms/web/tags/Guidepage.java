package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.model.Guide;
import com.flycms.module.config.service.GuideService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:38 2018/9/18
 */
@Service
public class Guidepage extends AbstractTagPlugin {

    @Autowired
    private GuideService guideService;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        // 获取页面的参数

        String name=null;

        Integer status=2;

        String orderby=null;

        String order=null;
        //翻页页数
        Integer p = 1;
        //每页记录条数
        Integer rows = 10;
        //处理标签变量
        Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
        for(String str:paramWrap.keySet()){
            if("name".equals(str)){
                name = paramWrap.get(str).toString();
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
            PageVo<Guide> pageVo = guideService.getGuideListPage(name,status,orderby,order,p,rows);
            env.setVariable("guide_page", builder.build().wrap(pageVo));
        } catch (Exception e) {
            env.setVariable("guide_page", builder.build().wrap(null));
        }
        body.render(env.getOut());
    }
}
