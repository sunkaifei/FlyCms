package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.user.model.UserMessage;
import com.flycms.module.user.service.UserMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信 REST（2026-10-23 补齐）。
 *
 * <p>背景：审核页一直弹「已驳回，并已站内信通知作者」，而在此之前站内信
 * 无表无实现 —— UI 承诺了不存在的能力。上一轮只做到「不再假承诺」，
 * 本控制器把能力补上，那句话才第一次成为真的。
 *
 * <p>权限节点（缺一不可，否则含超管在内全部 403）：
 * 900310 菜单 / 900311 发送 / 900315 删除 / 900313 标记已读。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Controller
@RequestMapping("/api")
public class ApiUserMessageController extends ApiBaseController {

    @Autowired
    private UserMessageService userMessageService;

    /** 站内信列表（后台视角：可按接收人/类型/已读/关键字过滤） */
    @ResponseBody
    @GetMapping("/system/message/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "msgType", required = false) Integer msgType,
                       @RequestParam(value = "isRead", required = false) Integer isRead,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/message/page");
        PageVo<UserMessage> pageVo =
                userMessageService.pageAdmin(userId, msgType, isRead, keyword, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 发送站内信。
     *
     * <p>{@code userIds} 为空或含 0 表示全体用户（受 Service 上限约束）。
     * 正文按纯文本处理：站内信不做富文本，避免再开一条 XSS 通道。
     */
    @ResponseBody
    @PostMapping("/system/message/send")
    public DataVo send(@RequestParam(value = "userIds", required = false) String userIds,
                       @RequestParam(value = "userId", required = false) Long userId,
                       @RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "content", required = false) String content) {
        requirePermission("/api/system/message/send");
        Admin admin = requireAdmin();
        List<Long> ids = parseIds(userIds);
        if (userId != null && userId > 0 && !ids.contains(userId)) {
            ids.add(userId);
        }
        DataVo r = userMessageService.sendSystem(ids, title, content,
                admin.getId(), admin.getAdminName());
        return r;
    }

    /** 后台删除站内信 */
    @ResponseBody
    @PostMapping("/system/message/delete")
    public DataVo delete(@RequestParam(value = "ids", defaultValue = "") String ids) {
        requirePermission("/api/system/message/delete");
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return DataVo.failure("请选择要删除的站内信");
        }
        int n = userMessageService.deleteAdmin(idList);
        return DataVo.success("已删除 " + n + " 条", n);
    }

    /**
     * 后台代标记已读（作者长期不登录时管理员可代清理未读）。
     * 前端用户自己读走 {@code /ucenter/message} 页面，不需要这个接口。
     */
    @ResponseBody
    @PostMapping("/system/message/read")
    public DataVo read(@RequestParam(value = "ids", required = false) String ids,
                       @RequestParam(value = "userId", required = false) Long userId) {
        requirePermission("/api/system/message/read");
        if (userId == null || userId <= 0) {
            return DataVo.failure("请指定用户");
        }
        int n = userMessageService.markRead(userId, parseIds(ids));
        return DataVo.success("已标记 " + n + " 条为已读", n);
    }

    /** 未读总数（后台概览角标） */
    @ResponseBody
    @GetMapping("/system/message/unread")
    public DataVo unread(@RequestParam(value = "userId", required = false) Long userId) {
        requirePermission("/api/system/message/page");
        if (userId == null || userId <= 0) {
            return DataVo.success("操作成功", 0);
        }
        return DataVo.success("操作成功", userMessageService.countUnread(userId));
    }

    private List<Long> parseIds(String ids) {
        List<Long> list = new java.util.ArrayList<>();
        if (StringUtils.isBlank(ids)) {
            return list;
        }
        for (String part : ids.split("[,;\\s]+")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            try {
                long n = Long.parseLong(v);
                if (n > 0 && !list.contains(n)) {
                    list.add(n);
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法 id
            }
        }
        return list;
    }
}
