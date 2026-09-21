package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.message.model.Message;
import com.flycms.module.message.service.MessageService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 站内短信管理 REST（对标帝国站内短信；复用既有 fly_message/MessageService，
 * is_admin=1 即"系统信息"语义与老系统一致）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiMessageController extends ApiBaseController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    /** 全站站内信分页（管理员视角），支持标题关键词过滤 */
    @ResponseBody
    @GetMapping("/system/message/list")
    public DataVo list(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "subject", required = false) String subject) throws Exception {
        requirePermission("/api/system/message/list");
        return DataVo.success("操作成功", messageService.getMessageListPage(
                null, null, StringUtils.trimToNull(subject),
                null, null, null, null, null, "send_time", "desc", pageNum, 20));
    }

    /**
     * 发送站内信（系统信息）：按收件人用户名解析用户，is_admin=1
     */
    @ResponseBody
    @PostMapping("/system/message/send")
    public DataVo send(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/message/send");
        String toUsername = StringUtils.trimToNull(params.get("toUsername"));
        String subject = StringUtils.trimToNull(params.get("subject"));
        String content = StringUtils.trimToNull(params.get("message"));
        if (toUsername == null || subject == null || content == null) {
            return DataVo.failure("收件人、标题、内容均不能为空");
        }
        User user = userService.findByUsername(toUsername);
        if (user == null) {
            return DataVo.failure("收件人用户不存在：" + toUsername);
        }
        Admin admin = com.flycms.core.utils.AdminSessionUtils.getLoginMember(request);
        Message message = new Message();
        message.setFromId(admin == null ? 0L : admin.getId());
        message.setToId(user.getUserId());
        message.setSubject(subject);
        message.setMessage(content);
        message.setIsAdmin(1);
        message.setState(1);
        message.setSendTime(new Date());
        message.setWriteTime(new Date());
        return messageService.addMessage(message);
    }

    @ResponseBody
    @PostMapping("/system/message/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") long id) {
        requirePermission("/api/system/message/delete");
        if (id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        messageService.deleteMessageById(id);
        return DataVo.success("已删除");
    }
}
