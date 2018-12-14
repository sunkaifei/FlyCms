package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:21 2018/7/13
 */
@Controller
@RequestMapping("/system/tools")
public class MySQLAdminController extends BaseController {
    //文章列表
    @GetMapping(value = "/db_bak")
    public String getDbBak(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("tools/db_bak");
    }
}
