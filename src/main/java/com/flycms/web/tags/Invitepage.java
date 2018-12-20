package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.user.model.UserInvite;
import com.flycms.module.user.service.UserInviteService;
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
 * 用户邀请注册记录列表标签
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:39 2018/9/15
 */
@Service
public class Invitepage extends AbstractTagPlugin {
    @Autowired
    private UserInviteService userInviteService;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        // 获取页面的参数
        //用户id
        Long userId = null;
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
            if("p".equals(str)){
                p = Integer.parseInt(paramWrap.get(str).toString());
            }
            if("rows".equals(str)){
                rows = Integer.parseInt(paramWrap.get(str).toString());
            }
        }
        // 获取文件的分页
        try {
            PageVo<UserInvite> pageVo = userInviteService.getUserInviteListPage(userId,status,orderby,order,p,rows);
            env.setVariable("invite_page", builder.build().wrap(pageVo));
        } catch (Exception e) {
            env.setVariable("invite_page", builder.build().wrap(null));
        }
        body.render(env.getOut());
    }
}
