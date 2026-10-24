package com.flycms.module.user.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.user.dao.UserMessageDao;
import com.flycms.module.user.model.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 用户站内信（2026-10-23 补齐）。
 *
 * <p>此前审核页弹「已驳回，并已站内信通知作者」，而全库无任何站内信实现 ——
 * 这是典型的「UI 跑在后端能力前面」。本服务把能力补上：
 * 审核驳回/通过 → {@link #sendAudit}；后台手工 → {@link #sendSystem}。
 *
 * <p>投递语义与边界：
 * <ul>
 *   <li>一行 = 一个接收者一条消息，广播在 Service 里展开成 N 行（不做 user_id=0 特例，
 *       否则已读状态无法按人区分）；</li>
 *   <li>正文是<b>纯文本</b>，前台一律转义输出，不接受 HTML —— 站内信不做富文本，
 *       避免再开一条 XSS 通道；</li>
 *   <li>单条失败只告警不回滚：通知是「告知」，不能反过来阻断审核动作本身。</li>
 * </ul>
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Slf4j
@Service
public class UserMessageService {

    /** 标题列宽 */
    private static final int MAX_TITLE = 250;
    /** 正文列宽 */
    private static final int MAX_CONTENT = 2000;
    /** 单次广播上限：防止误操作把全表刷爆 */
    private static final int MAX_BROADCAST = 5000;

    @Autowired
    private UserMessageDao userMessageDao;

    /**
     * 发送一条站内信。
     *
     * @param userId 接收者；为 null 或 &lt;=0 视为无有效接收者，直接返回失败（不静默丢弃）
     * @return 成功返回消息 id
     */
    public DataVo send(Long userId, int msgType, String title, String content,
                       String targetModel, Long targetId, Long senderId, String senderName) {
        if (userId == null || userId <= 0) {
            return DataVo.failure("接收用户不存在，站内信未发送");
        }
        String t = trim(title, MAX_TITLE);
        if (t.isEmpty()) {
            return DataVo.failure("站内信标题不能为空");
        }
        UserMessage m = new UserMessage();
        m.setId(SnowFlake.getInstance().nextId());
        m.setUserId(userId);
        m.setMsgType(normalizeType(msgType));
        m.setTitle(t);
        m.setContent(trimToNull(content, MAX_CONTENT));
        m.setTargetModel(StringUtils.isBlank(targetModel) ? null : targetModel.trim());
        m.setTargetId(targetId);
        m.setSenderId(senderId);
        m.setSenderName(StringUtils.isBlank(senderName) ? null : trim(senderName, 64));
        m.setCreateTime(new Date());
        userMessageDao.addMessage(m);
        return DataVo.success("发送成功", m.getId());
    }

    /**
     * 审核结果通知（驳回/通过）。失败只告警，绝不阻断审核动作。
     *
     * @param passed true=通过 false=驳回
     * @return 是否成功投递
     */
    public boolean sendAudit(Long userId, boolean passed, String title, String reason,
                             String targetModel, Long targetId, Long auditorId, String auditorName) {
        if (userId == null || userId <= 0) {
            // 后台代发的内容没有作者，属正常情况，不算失败
            return false;
        }
        try {
            String head = passed ? "内容已通过审核" : "内容未通过审核";
            StringBuilder body = new StringBuilder();
            body.append("内容：").append(StringUtils.defaultString(title)).append('\n');
            if (passed) {
                body.append("结果：已通过并发布");
            } else {
                body.append("结果：未通过").append('\n');
                body.append("原因：").append(StringUtils.defaultString(trim(reason, MAX_CONTENT - 32), "（未填写）"));
            }
            DataVo r = send(userId, UserMessage.Type.AUDIT, head, body.toString(),
                    targetModel, targetId, auditorId, auditorName);
            return r.getCode() == DataVo.CODE_SUCCESS;
        } catch (Exception e) {
            log.warn("审核站内信发送失败：userId={} target={}#{} {}",
                    userId, targetModel, targetId, e.getMessage());
            return false;
        }
    }

    /** 后台手工发送（可广播）。userIds 为空表示全体用户，受 {@link #MAX_BROADCAST} 限制。 */
    public DataVo sendSystem(List<Long> userIds, String title, String content,
                             Long senderId, String senderName) {
        String t = trim(title, MAX_TITLE);
        if (t.isEmpty()) {
            return DataVo.failure("标题不能为空");
        }
        List<Long> targets = userIds == null ? new ArrayList<>() : distinct(userIds);
        if (targets.isEmpty()) {
            return DataVo.failure("请指定接收用户");
        }
        if (targets.size() > MAX_BROADCAST) {
            return DataVo.failure("单次发送不得超过 " + MAX_BROADCAST + " 人");
        }
        int ok = 0;
        for (Long uid : targets) {
            try {
                DataVo r = send(uid, UserMessage.Type.SYSTEM, t, content,
                        null, null, senderId, senderName);
                if (r.getCode() == DataVo.CODE_SUCCESS) {
                    ok++;
                }
            } catch (Exception e) {
                log.warn("站内信发送失败：userId={} {}", uid, e.getMessage());
            }
        }
        return DataVo.success("已发送 " + ok + "/" + targets.size() + " 条", ok);
    }

    /** 我的消息分页 */
    public PageVo<UserMessage> pageMine(Long userId, Integer isRead, int pageNum, int rows) {
        PageVo<UserMessage> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        int offset = (Math.max(1, pageNum) - 1) * rows;
        List<UserMessage> list = userMessageDao.selectByUser(userId, isRead, offset, rows);
        pageVo.setList(list == null ? new ArrayList<>() : list);
        pageVo.setCount(userMessageDao.countByUser(userId, isRead));
        return pageVo;
    }

    public int countUnread(Long userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        return userMessageDao.countUnread(userId);
    }

    /** 标记已读：ids 为空表示全部已读 */
    public int markRead(Long userId, List<Long> ids) {
        return (ids == null || ids.isEmpty())
                ? userMessageDao.markAllRead(userId)
                : userMessageDao.markRead(userId, ids);
    }

    /** 删除自己的消息 */
    public int deleteMine(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return userMessageDao.deleteByUser(userId, ids);
    }

    /** 后台分页 */
    public PageVo<UserMessage> pageAdmin(Long userId, Integer msgType, Integer isRead,
                                         String keyword, int pageNum, int rows) {
        PageVo<UserMessage> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        int offset = (Math.max(1, pageNum) - 1) * rows;
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        List<UserMessage> list =
                userMessageDao.selectAdminPage(userId, msgType, isRead, kw, offset, rows);
        pageVo.setList(list == null ? new ArrayList<>() : list);
        pageVo.setCount(userMessageDao.countAdminPage(userId, msgType, isRead, kw));
        return pageVo;
    }

    /** 后台删除 */
    public int deleteAdmin(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return userMessageDao.deleteByIds(ids);
    }

    /** 保留策略：清理 days 天前已读的站内信 */
    public int cleanReadBefore(int days) {
        return userMessageDao.deleteReadBefore(Math.max(1, days));
    }

    // /////////////////// 内部 ///////////////////

    private int normalizeType(int msgType) {
        return (msgType == UserMessage.Type.AUDIT || msgType == UserMessage.Type.INTERACT)
                ? msgType
                : UserMessage.Type.SYSTEM;
    }

    private String trim(String v, int max) {
        String s = StringUtils.trimToEmpty(v);
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String trimToNull(String v, int max) {
        if (StringUtils.isBlank(v)) {
            return null;
        }
        return trim(v, max);
    }

    private List<Long> distinct(List<Long> ids) {
        List<Long> out = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && id > 0 && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }
}
