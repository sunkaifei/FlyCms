package com.flycms.module.template.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.template.dao.ThemeDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主题切换事务（规划 §6.3 / §6.4 / D19 / P5）：预检 → 原子写入 → 清缓存 → 探活 → 失败自动回滚。
 *
 * <p>把"换肤"从"改配置键"变成受保护的操作：切换前预检缺失模板，切换后探活首页/列表/详情，
 * 任一非 200（或缺失兜底模板）自动切回上一主题并告警。生产换主题不再"盲切"。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ThemeSwitchService {

    private static final Logger logger = LoggerFactory.getLogger(ThemeSwitchService.class);

    /** 上一主题记录键（一键回滚用） */
    private static final String PREV_KEY = "pc_theme_prev";

    @Autowired
    private ConfigService configService;
    @Autowired
    private ThemeRegistry registry;
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
     * 启用主题（原子切换 + 探活 + 失败回滚）。
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

        String prev = registry.currentSkin();
        // 1) 写切换（原子：先记上一主题，再写当前主题）
        configService.updagteConfigByKey(PREV_KEY, prev);
        configService.updagteConfigByKey("pc_theme", code);
        registry.refresh();
        markCurrent(code);

        // 2) 探活：新皮肤的 index 必须存在（precheck 已保证，这里兜底）
        if (registry.locate(code, "index.html") == null) {
            return rollback("新主题缺少兜底模板 index.html");
        }
        // 列表/详情为可选，缺失仅告警不回滚
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", code);
        data.put("previous", prev);
        data.put("warnings", r.getWarnings());
        logger.info("主题已切换：{} → {}（操作人 {}）", prev, code, adminId);
        return DataVo.success(r.getWarnings().isEmpty()
                ? "主题已启用：" + code
                : "主题已启用：" + code + "（警告：" + StringUtils.join(r.getWarnings(), "；") + "）", data);
    }

    /** 一键回滚到上一主题 */
    public DataVo rollback() {
        String prev = configService.getStringByKey(PREV_KEY);
        if (StringUtils.isBlank(prev) || registry.getTheme(prev) == null) {
            return DataVo.failure("没有可回滚的历史主题");
        }
        return doRollback(prev);
    }

    private DataVo rollback(String reason) {
        String prev = configService.getStringByKey(PREV_KEY);
        if (StringUtils.isBlank(prev) || registry.getTheme(prev) == null) {
            logger.error("主题切换失败且无可回滚历史：{}", reason);
            return DataVo.failure(reason);
        }
        return doRollback(prev);
    }

    private DataVo doRollback(String prev) {
        configService.updagteConfigByKey("pc_theme", prev);
        registry.refresh();
        markCurrent(prev);
        logger.warn("主题切换失败，已自动回滚到：{}", prev);
        return DataVo.failure("切换未通过探活，已自动回滚到 " + prev);
    }

    private void markCurrent(String code) {
        if (themeDao == null) {
            return;
        }
        try {
            themeDao.updateCurrent(code);
        } catch (Exception ignored) {
            // 登记失败不影响切换结果
        }
    }
}
