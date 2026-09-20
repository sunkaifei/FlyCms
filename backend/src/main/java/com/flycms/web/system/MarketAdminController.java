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
 * @Date: 10:14 2018/7/7
 */
@Controller
@RequestMapping("/system/market")
public class MarketAdminController extends BaseController{

    //促销活动列表
    @GetMapping(value = "/pro_rule_list")
    public String getProRuleList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/pro_rule_list");
    }

    //积分兑换列表
    @GetMapping(value = "/cost_point_list")
    public String getCostPointList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/cost_point_list");
    }

    //限时抢购列表
    @GetMapping(value = "/pro_speed_list")
    public String getProSpeedList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/pro_speed_list");
    }

    //团购列表
    @GetMapping(value = "/regiment_list")
    public String getRegimentList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/regiment_list");
    }

    //特价活动列表
    @GetMapping(value = "/sale_list")
    public String getSaleList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/sale_list");
    }

    //代金券列表列表
    @GetMapping(value = "/ticket_list")
    public String getTicketList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("market/ticket_list");
    }
}
