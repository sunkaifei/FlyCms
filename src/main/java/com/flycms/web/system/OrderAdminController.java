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
 * @Date: 17:10 2018/7/5
 */
@Controller
@RequestMapping("/system/order")
public class OrderAdminController  extends BaseController {
    //订单列表
    @GetMapping(value = "/order_list")
    public String getOrderList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("order/order_list");
    }

    //添加订单
    @GetMapping(value = "/order_add")
    public String getOrderAdd(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("order/order_add");
    }
}
