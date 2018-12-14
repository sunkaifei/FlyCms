package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.module.topic.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:05 2018/9/3
 */
@Controller
@RequestMapping("/system/topic")
public class TopicsAdminController extends BaseController {
    @Autowired
    private TopicService topicService;

    @GetMapping(value = "/list_topics")
    public String questionList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_topics");
    }

}
