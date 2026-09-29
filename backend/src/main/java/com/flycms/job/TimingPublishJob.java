package com.flycms.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 定时发布 Job（规划阶段 H，对标帝国定时审核）：
 * 每分钟扫描全部 fly_cmodel_{模型} 中 status=4 且 publish_time<=now 的内容置为发布（U3 后 article 也走模型表）。
 * 发布即生效红线（§6.4）：到点自动可见，无需任何人工刷新。
 */
@Component
public class TimingPublishJob {

    private static final Logger log = LoggerFactory.getLogger(TimingPublishJob.class);

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void publishDue() {
        try {
            List<Map<String, Object>> models = jdbcTemplate.queryForList(
                    "select id, code from fly_model where status = 1");
            for (Map<String, Object> m : models) {
                Object code = m.get("code");
                if (code == null || String.valueOf(code).isBlank()) {
                    continue;
                }
                // 物理表名 = fly_cmodel_{模型 code}（见 ModelDataDao.xml / SqlSafeUtil）。
                // 早期这里误用 m.get("id") → 拼出 fly_cmodel_3 这种不存在的表，
                // 异常被 catch 吞掉，定时发布**静默失效**（到点不发布且无任何提示）。
                String table = "fly_cmodel_" + code;
                try {
                    int n = jdbcTemplate.update(
                            "update `" + table + "` set status = 1 " +
                            "where status = 4 and publish_time is not null and publish_time <= now()");
                    if (n > 0) {
                        log.info("定时发布 {}: {} 条", table, n);
                    }
                } catch (Exception e) {
                    // 表不存在（模型未建表）忽略，仅 DEBUG 留痕
                    log.debug("定时发布跳过 {}：{}", table, e.getMessage());
                }
            }
            // U3：fly_article 硬编码块已删——articles 模型同样由上面的 fly_cmodel_{code} 循环覆盖
        } catch (Exception e) {
            log.error("定时发布执行失败", e);
        }
    }
}
