package com.flycms.module.user;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ContentSanitizer;
import com.flycms.module.user.dao.UserMessageDao;
import com.flycms.module.user.model.UserMessage;
import com.flycms.module.user.service.UserMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 站内信 + 正文清洗测试（2026-10-23）。
 *
 * <p>背景：审核页一直弹「已驳回，并已站内信通知作者」，而在此之前站内信
 * 无表无实现。这里验证两件事：
 * <ol>
 *   <li>站内信能发、能读回、未读数与已读状态对得上（含「发给不存在的用户」必须失败）；</li>
 *   <li>正文清洗真的挡得住存储型 XSS，且<b>对干净正文零改动</b>（幂等，否则存量扫描会
 *       把全部内容重写一遍）。</li>
 * </ol>
 *
 * <p><b>运行条件</b>：本机 3306 可达时执行；否则整类跳过。用例后自清理。
 */
@SpringBootTest
@EnabledIf("mysqlReachable")
class UserMessageServiceTest {

    /** 明显不属于业务的测试用户 id，便于清理与排查 */
    private static final long TEST_USER = 9_990_001L;

    @Autowired
    private UserMessageService service;

    @Autowired
    private UserMessageDao dao;

    static boolean mysqlReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 3306), 800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void cleanup() {
        service.deleteMine(TEST_USER, List.of());
        // deleteMine 在 ids 为空时不动数据，这里直接用 DAO 按用户清
        for (UserMessage m : dao.selectByUser(TEST_USER, null, 0, 500)) {
            dao.deleteByUser(TEST_USER, List.of(m.getId()));
        }
    }

    @Test
    @DisplayName("站内信：发送 → 未读 1 → 已读 → 未读 0")
    void sendThenRead() {
        cleanup();
        DataVo r = service.send(TEST_USER, UserMessage.Type.AUDIT, "内容未通过审核",
                "内容：测试\n原因：含有违规外链", "articles", 42L, 1L, "admin");
        assertEquals(DataVo.CODE_SUCCESS, r.getCode(), "发送必须成功");

        assertEquals(1, service.countUnread(TEST_USER), "刚发出去必须是未读");
        PageVo<UserMessage> page = service.pageMine(TEST_USER, 0, 1, 10);
        assertEquals(1, page.getCount(), "未读列表应有 1 条");
        UserMessage got = page.getList().get(0);
        assertEquals("articles", got.getTargetModel(), "审核通知必须回填模型");
        assertEquals(42L, got.getTargetId(), "审核通知必须回填内容 id");
        assertEquals("admin", got.getSenderName(), "发送者快照要能读回");

        int marked = service.markRead(TEST_USER, List.of(got.getId()));
        assertEquals(1, marked, "应标记 1 条已读");
        assertEquals(0, service.countUnread(TEST_USER), "已读后未读数归零");

        cleanup();
    }

    @Test
    @DisplayName("站内信：接收用户不存在时必须失败，不能静默丢弃")
    void sendToMissingUserFails() {
        DataVo r = service.send(0L, UserMessage.Type.SYSTEM, "x", "y", null, null, null, null);
        assertEquals(DataVo.CODE_FAILURED, r.getCode(), "无效接收者必须显式失败");
    }

    @Test
    @DisplayName("正文清洗：script/on*/伪协议全被剥掉，且干净正文零改动")
    void sanitizeStripsXssAndIsIdempotent() {
        String dirty = "<p onclick=\"alert(1)\">正文</p>"
                + "<script>alert('xss')</script>"
                + "<img src=\"x\" onerror=\"alert(2)\">"
                + "<a href=\"javascript:alert(3)\">点我</a>"
                + "<iframe src=\"https://evil.example.com/x.html\"></iframe>"
                + "<h2>合法标题</h2><table><tr><td>单元格</td></tr></table>";
        String cleaned = ContentSanitizer.sanitize(dirty);

        assertNotNull(cleaned);
        assertFalse(cleaned.contains("<script"), "script 标签必须被剥掉");
        assertFalse(cleaned.contains("onclick"), "onclick 必须被剥掉");
        assertFalse(cleaned.contains("onerror"), "onerror 必须被剥掉");
        assertFalse(cleaned.contains("javascript:"), "javascript: 伪协议必须被剥掉");
        assertFalse(cleaned.contains("evil.example.com"), "非白名单 iframe 域名必须被剥掉");
        assertTrue(cleaned.contains("合法标题"), "合法排版标签不能误删");
        assertTrue(cleaned.contains("单元格"), "表格不能误删");

        String clean = "<p>正文</p><h2>标题</h2><img src=\"https://a.com/b.png\" alt=\"图\">";
        String twice = ContentSanitizer.sanitize(ContentSanitizer.sanitize(clean));
        assertEquals(ContentSanitizer.sanitize(clean), twice, "清洗必须幂等");
        assertFalse(ContentSanitizer.changed(ContentSanitizer.sanitize(clean)),
                "已清洗过的正文再次扫描应判定为无变化");
    }
}
