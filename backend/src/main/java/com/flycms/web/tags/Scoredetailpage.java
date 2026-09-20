package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.score.model.ScoreDetail;
import com.flycms.module.score.model.ScoreRule;
import com.flycms.module.score.service.ScoreDetailService;
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
 * @Date: 19:13 2018/9/3
 */
@Service
public class Scoredetailpage extends AbstractTagPlugin {

    @Autowired
    private ScoreDetailService scoreDetailService;

    @SuppressWarnings("rawtypes")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        // 获取页面的参数
        Long userId = null;

        Integer status = null;

        String orderby=null;

        String order=null;
        //翻页页数
        Integer p = 1;
        //每页记录条数
        Integer rows = 10;
        @SuppressWarnings("unchecked")
        Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
        for(String str:paramWrap.keySet()){
            if("userId".equals(str)){
                userId = Long.parseLong(paramWrap.get(str).toString());
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
            if("status".equals(str)){
                status = Integer.parseInt(paramWrap.get(str).toString());
            }
            if("p".equals(str)){
                p = Integer.parseInt(paramWrap.get(str).toString());
            }
            if("rows".equals(str)){
                rows = Integer.parseInt(paramWrap.get(str).toString());
            }
        }
        // 获取文件的分页
        PageVo<ScoreDetail> detail_page = scoreDetailService.scoreDetaillistPage(userId, status, orderby, order, p, rows);
        env.setVariable("detail_page", builder.build().wrap(detail_page));
        body.render(env.getOut());
    }

}