package com.flycms.module.model;

import com.flycms.module.model.dao.ContentAuditLogDao;
import com.flycms.module.model.model.ContentAuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内容审核日志 DAO 测试（2026-10-23）。
 *
 * <p>背景：审核页一直把驳回原因传给 {@code /api/system/audit/audit|batch}，
 * 但控制器形参列表里没有 reason，Spring 只按形参绑定 → 原因被静默丢弃，
 * 而 UI 仍弹「已驳回，并已站内信通知作者」。这里用真实 MySQL 验证
 * 「原因确实能落库、能按内容读回」，防止再退回"界面说有、库里没有"。
 *
 * <p><b>运行条件</b>：本机 3306 可达时执行（读 application.yml 的数据源）；
 * 否则整类跳过，不因缺库失败。用完即清理，不污染业务数据。
 */
@SpringBootTest
@EnabledIf("mysqlReachable")
class ContentAuditLogDaoTest {

    /** 明显不属于业务的测试目标 id，便于清理与排查 */
    private static final String TARGET_MODEL = "__audit_log_test__";
    private static final long TARGET_ID = 1L;

    @Autowired
    private ContentAuditLogDao dao;

    /** 3306 可连才跑（无库环境跳过而不是失败） */
    static boolean mysqlReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 3306), 800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("驳回原因写入后可按内容读回，审核人与标题快照不丢")
    void addLogThenSelectByTarget() {
        dao.deleteByTargetModel(TARGET_MODEL);

        ContentAuditLog log = new ContentAuditLog();
        log.setId(999_000_001L);
        log.setTargetModel(TARGET_MODEL);
        log.setTargetId(TARGET_ID);
        log.setTitle("标题快照");
        log.setAction(2);
        log.setReason("内容含未标注来源的转载");
        log.setAuditorId(272843906771857408L);
        log.setAuditorName("flycms");
        log.setCreateTime(new Date());

        assertEquals(1, dao.addLog(log), "审核日志应写入 1 行");

        List<ContentAuditLog> rows = dao.selectByTarget(TARGET_MODEL, TARGET_ID, 0, 10);
        assertEquals(1, rows.size(), "应能按内容读回 1 条审核日志");
        ContentAuditLog back = rows.get(0);
        assertNotNull(back);
        assertEquals(2, back.getAction(), "action 必须为 2（驳回）");
        assertEquals("内容含未标注来源的转载", back.getReason(), "驳回原因必须原样落库");
        assertEquals("标题快照", back.getTitle(), "标题快照应留档（内容删除后仍可追溯）");
        assertEquals("flycms", back.getAuditorName(), "审核人应留档");
        assertEquals(1, dao.countByTarget(TARGET_MODEL, TARGET_ID));

        // 收尾：不留测试数据
        dao.deleteByTargetModel(TARGET_MODEL);
        assertEquals(0, dao.countByTarget(TARGET_MODEL, TARGET_ID));
    }
}
