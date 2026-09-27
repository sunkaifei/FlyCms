package com.flycms.module.template.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.template.dao.ThemeDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主题切换事务（规划 §6.3 / §6.4 / D19 / P5）：预检 → 前置校验 → 原子写入 → 清缓存 → 真实探活 → 失败自动回滚。
 *
 * <p>把"换肤"从"改一个配置键"变成受保护的操作：
 * <ol>
 *   <li>切换前预检缺失模板（阻断性缺 index.html 直接拒绝）；</li>
 *   <li>切换前校验<b>回滚点是否有效</b>——当前主题必须是磁盘上真实存在的主题，
 *       否则不写任何配置就中止（2026-09-28 修复：原先会把脏数据写进 pc_theme_prev，
 *       导致"一键回滚"在之后永久失效，而恰恰是第一次切换就失败时最需要它）；</li>
 *   <li>写入新主题后发起<b>真实 HTTP 探活</b>，非 200 或页面含模板报错即自动回滚；</li>
 *   <li>回滚后再探一次，避免"回滚了但还是白屏"被当成功。</li>
 * </ol>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ThemeSwitchService {

    private static final Logger logger = LoggerFactory.getLogger(ThemeSwitchService.class);

    /** 上一主题记录键（一键回滚用） */
    private static final String PREV_KEY = "pc_theme_prev";

    /** 当前主题配置键 */
    private static final String CURRENT_KEY = "pc_theme";

    /** 手动回滚时传给 doRollback 的原因，用于拼装提示文案 */
    private static final String REASON_MANUAL = "手动一键回滚";

    @Autowired
    private ConfigService configService;
    @Autowired
    private ThemeRegistry registry;
    @Autowired
    private ThemeHealthChecker healthChecker;
    @Autowired(required = false)
    private ThemeDao themeDao;

    /** 兼容性预检（不切换，仅返回结果） */
    public DataVo check(String code) {
        if (StringUtils.isBlank(code) || registry.getTheme(code) == null) {
            return DataVo.failure("主题不存在：" + code);
        }
        ThemeRegistry.CompatResult r = registry.checkCompatibility(code);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("ok", r.isOk());
        data.put("blocks", r.getBlocks());
        data.put("warnings", r.getWarnings());
        if (r.isOk()) {
            return DataVo.success("兼容性检查通过", data);
        }
        return DataVo.failure("存在阻断性问题，禁止切换", data);
    }

    /** 预览地址：管理员带此参数访问，只影响自己会话，访客仍看旧主题（§6.3） */
    public String previewUrl(String code) {
        return "/?__skin=" + code;
    }

    /**
     * 启用主题（前置校验 + 原子切换 + 真实探活 + 失败回滚）。
     *
     * @param code    目标主题目录名
     * @param adminId 操作人（审计用）
     */
    public DataVo enable(String code, Long adminId) {
        if (StringUtils.isBlank(code) || registry.getTheme(code) == null) {
            return DataVo.failure("主题不存在：" + code);
        }
        ThemeRegistry.CompatResult r = registry.checkCompatibility(code);
        if (!r.isOk()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("blocks", r.getBlocks());
            data.put("warnings", r.getWarnings());
            return DataVo.failure("兼容性预检未通过：" + StringUtils.join(r.getBlocks(), "；"), data);
        }

        // 0) 回滚点前置校验：当前主题必须真实存在，否则写进去的是脏回滚点
        String prev = registry.currentSkin();
        if (StringUtils.isBlank(prev) || registry.getTheme(prev) == null) {
            logger.error("主题切换中止：当前主题 [{}] 不是磁盘上存在的主题，无法确定回滚点", prev);
            return DataVo.failure("当前主题状态异常（" + CURRENT_KEY + "=" + prev
                    + "），无法确定回滚点，已中止切换。请先在站点设置中把当前主题修正为存在的主题。");
        }

        // 1) 原子写入：先记回滚点，再写当前主题
        configService.updagteConfigByKey(PREV_KEY, prev);
        configService.updagteConfigByKey(CURRENT_KEY, code);
        registry.refresh();
        markCurrent(code);

        // 2) 真实探活：自请求首页，非 200 或页面含模板报错即回滚
        List<String> warnings = new ArrayList<>(r.getWarnings());
        ThemeHealthChecker.ProbeResult probe = healthChecker.probe();
        if (!probe.ok) {
            return rollback(probe.message);
        }
        if (!probe.conclusive) {
            // 探活没能执行（请求未到达应用）：放行但必须留痕，不能假装探活成功了
            warnings.add("探活未能完成（" + probe.message + "），已放行，建议人工打开首页确认");
            logger.warn("主题 [{}] 切换后探活未能完成，已放行：{}", code, probe.message);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", code);
        data.put("previous", prev);
        data.put("warnings", warnings);
        logger.info("主题已切换：{} → {}（操作人 {}）", prev, code, adminId);
        return DataVo.success(warnings.isEmpty()
                ? "主题已启用：" + code
                : "主题已启用：" + code + "（警告：" + StringUtils.join(warnings, "；") + "）", data);
    }

    /** 一键回滚到上一主题（管理员手动触发） */
    public DataVo rollback() {
        String prev = configService.getStringByKey(PREV_KEY);
        if (StringUtils.isBlank(prev) || registry.getTheme(prev) == null) {
            return DataVo.failure("没有可回滚的历史主题");
        }
        return doRollback(prev, REASON_MANUAL);
    }

    /**
     * 探活失败后的自动回滚。
     *
     * @param reason 失败原因（来自探活结果）
     */
    private DataVo rollback(String reason) {
        String prev = configService.getStringByKey(PREV_KEY);
        if (StringUtils.isBlank(prev) || registry.getTheme(prev) == null) {
            // 进入切换流程前已校验过回滚点，走到这里说明回滚点被并发改坏了：
            // 必须显式区分"没回滚"和"回滚了"，不能只回一句失败让人以为已经恢复。
            logger.error("主题切换失败，且回滚点 [{}] 无效，未能自动回滚：{}", prev, reason);
            return DataVo.failure(reason
                    + "；且没有可用的历史主题，未能自动回滚，请立即人工检查 " + CURRENT_KEY + " 配置");
        }
        return doRollback(prev, reason);
    }

    /**
     * 执行回滚，并在回滚后复探一次确认服务真的恢复了。
     *
     * @param prev   回滚目标主题
     * @param reason 触发回滚的原因
     */
    private DataVo doRollback(String prev, String reason) {
        configService.updagteConfigByKey(CURRENT_KEY, prev);
        registry.refresh();
        markCurrent(prev);

        ThemeHealthChecker.ProbeResult after = healthChecker.probe();
        if (!after.ok) {
            logger.error("【严重】已回滚到 {}，但回滚后探活仍然失败：{}", prev, after.message);
            return DataVo.failure(reason + "；已回滚到 " + prev + "，但回滚后探活仍然失败（"
                    + after.message + "），请立即人工介入");
        }

        logger.warn("主题切换失败，已回滚到 {}（原因：{}）", prev, reason);
        return DataVo.failure(REASON_MANUAL.equals(reason)
                ? "已回滚到上一主题：" + prev
                : reason + "；已自动回滚到 " + prev);
    }

    /**
     * 同步 fly_theme.is_current 标记。
     *
     * <p>该标记仅用于后台展示，必须与 {@code pc_theme} 保持一致。
     * 登记失败不影响切换结果，但原先的 {@code catch (Exception ignored)} 会让"库里标记和实际主题不一致"
     * 这件事无迹可查，故改为记警告日志。
     */
    private void markCurrent(String code) {
        if (themeDao == null) {
            return;
        }
        try {
            themeDao.clearCurrent();
            themeDao.updateCurrent(code);
        } catch (Exception e) {
            logger.warn("主题 is_current 登记失败（不影响切换结果）：code={}，原因={}", code, e.getMessage());
        }
    }
}
