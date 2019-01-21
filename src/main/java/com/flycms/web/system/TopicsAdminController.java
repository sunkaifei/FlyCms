package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    public String topicList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_topics");
    }

    //添加话题
    @GetMapping(value = "/add")
    public String getAddTopic(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("content/add_topic");
    }

    //保存添加文章
    @PostMapping("/ucenter/article/article_save")
    @ResponseBody
    public DataVo addAdminSave(@Valid Topic topic, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = topicService.addTopic(topic);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
