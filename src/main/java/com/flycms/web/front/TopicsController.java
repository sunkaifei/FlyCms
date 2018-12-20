package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.model.TopicEdit;
import com.flycms.module.topic.service.TopicService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @Date: 10:06 2018/9/3
 */
@Controller
public class TopicsController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(TopicsController.class);
    @Autowired
    private TopicService topicService;
    /**
     * 话题首页
     *
     * @return
     */
    @GetMapping(value = {"/topics/"})
    public String topics(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        return theme.getPcTemplate("topics/index");
    }

    //话题详细页面
    @GetMapping(value = "/topics/{shortUrl}")
    public String findTopicById(@RequestParam(value = "p", defaultValue = "1") int p,@PathVariable(value = "shortUrl", required = false) String shortUrl, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        Topic topic = topicService.findTopicByShorturl(shortUrl);
        if(topic==null){
            return theme.getPcTemplate("404");
        }
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("topic", topic);
        return theme.getPcTemplate("topics/detail");
    }

    //处理关注信息
    @ResponseBody
    @PostMapping(value = "/topics/follow")
    public DataVo topicsFollow(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("话题id参数错误");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            data=topicService.addTopicAndUser(getUser().getUserId(),Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑话题资源
    @GetMapping(value = "/ucenter/topics/edit-{id}")
    public String editTopics(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        Topic topic = topicService.findTopicById(Long.parseLong(id),2);
        if(topic==null){
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("topic", topic);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("topics/edit_topic");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/topics/topic_update")
    public DataVo editShare(TopicEdit topicEdit, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        if (topicEdit.getTopicId()==null || "".equals(topicEdit.getTopicId())) {
            return DataVo.failure("话题id错误！");
        }
        Topic topic = topicService.findTopicById(topicEdit.getTopicId(),2);
        if (topic == null) {
            return DataVo.failure("话题不存在！");
        }
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            topicEdit.setUserId(getUser().getUserId());
            data = topicService.addUserEditTopic(topicEdit);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
