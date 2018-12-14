package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.message.model.Message;
import com.flycms.module.message.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
 * @Date: 12:12 2018/11/14
 */
@Slf4j
public class MessageController extends BaseController {
    @Autowired
    private MessageService messageService;

    //我的站内短信
    @GetMapping(value = "/ucenter/message")
    public String userMessage(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/message");
    }

    //保存用户发送的短信
    @PostMapping("/ucenter/message/add_message")
    @ResponseBody
    public DataVo addMessage(@Valid Message message, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            message.setFromId(getUser().getUserId());
            data = messageService.addMessage(message);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
