package com.flycms.module.template.service;

import com.flycms.module.config.service.ConfigService;
import com.flycms.module.template.dao.ThemeDao;
import com.flycms.module.template.model.Theme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 主题注册表启动期一致性校验（规划 §16 风险 3 的对策）。
 *
 * <p><b>为什么需要它</b>：{@code fly_theme} 是"扫描结果的登记"，事实源始终是磁盘 {@code theme.json}。
 * 原实现只有 {@code ThemeRegistry} 的 60s TTL <b>惰性</b>扫描 + upsert，只做"加"，不做"减"——
 * 手工删掉主题目录后，DB 里的登记会永久残留，主题市场就出现"看得见、点不动"的幽灵主题。
 *
 * <p>本 Runner 在启动时做三件事（全部 best-effort，绝不阻断启动）：
 * <ol>
 *   <li><b>登记</b>：强制扫描一次磁盘主题并 upsert 进 {@code fly_theme}；</li>
 *   <li><b>清理</b>：删除"磁盘已不存在"的脏登记；</li>
 *   <li><b>自愈</b>：{@code pc_theme} 为空或指向不存在的主题时，修正为存在的主题
 *       （首装场景；否则 {@code ThemeSwitchService.enable} 会因回滚点校验而拒绝一切切换）。</li>
 * </ol>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
@Order(1000)
public class ThemeBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ThemeBootstrap.class);

    /** 当前主题配置键 */
    private static final String CURRENT_KEY = "pc_theme";

    /** 首装自愈时的优先候选（按顺序取第一个磁盘存在的） */
    private static final List<String> PREFERRED = java.util.Arrays.asList("defalut", "default");

    @Autowired
    private ThemeRegistry registry;
    @Autowired
    private ConfigService configService;
    @Autowired(required = false)
    private ThemeDao themeDao;

    @Override
    public void run(ApplicationArguments args) {
        try {
            reconcile();
        } catch (Exception e) {
            // 主题登记是辅助能力，绝不能因为它让应用起不来
            logger.warn("主题注册表启动校验失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 执行一次完整的登记 + 清理 + 自愈 */
    public void reconcile() {
        // 1) 强制重新扫描（同时把磁盘主题 upsert 进 fly_theme）
        registry.refresh();
        List<Theme> onDisk = registry.allThemes();
        Set<String> diskCodes = new HashSet<>();
        for (Theme t : onDisk) {
            if (StringUtils.isNotBlank(t.getCode())) {
                diskCodes.add(t.getCode());
            }
        }
        logger.info("主题注册表启动校验：磁盘主题 {} 个 [{}]", diskCodes.size(), String.join(", ", diskCodes));

        if (onDisk.isEmpty()) {
            logger.error("主题根目录下没有任何主题，前台将无法渲染，请检查 {}/", ThemeRegistry.THEME_ROOT);
            return;
        }

        // 2) 清理磁盘上已不存在的脏登记
        if (themeDao != null) {
            try {
                List<Theme> rows = themeDao.listAll();
                int removed = 0;
                if (rows != null) {
                    for (Theme row : rows) {
                        if (row != null && StringUtils.isNotBlank(row.getCode())
                                && !diskCodes.contains(row.getCode())) {
                            themeDao.deleteByCode(row.getCode());
                            removed++;
                            logger.info("清理脏主题登记：{}（磁盘上已不存在）", row.getCode());
                        }
                    }
                }
                if (removed > 0) {
                    logger.info("主题注册表启动清理完成，共移除 {} 条脏登记", removed);
                }
            } catch (Exception e) {
                logger.warn("主题脏登记清理失败（不影响启动）：{}", e.getMessage());
            }
        }

        // 3) 自愈 pc_theme：空 / 不存在 → 修正为磁盘上存在的主题
        String cur = configService.getStringByKey(CURRENT_KEY);
        if (StringUtils.isNotBlank(cur) && diskCodes.contains(cur)) {
            markCurrent(cur);
            return;
        }
        String repair = pickRepairTarget(diskCodes);
        if (repair == null) {
            return;
        }
        logger.warn("当前主题配置 [{}] 不可用（{}），启动自愈为 [{}]",
                cur, StringUtils.isBlank(cur) ? "未设置" : "磁盘上不存在", repair);
        try {
            configService.updagteConfigByKey(CURRENT_KEY, repair);
            registry.refresh();
            markCurrent(repair);
        } catch (Exception e) {
            logger.warn("pc_theme 启动自愈失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 选一个可用的修复目标：优先 defalut/default，否则取字母序第一个 */
    private String pickRepairTarget(Set<String> diskCodes) {
        for (String p : PREFERRED) {
            if (diskCodes.contains(p)) {
                return p;
            }
        }
        return diskCodes.stream().sorted().findFirst().orElse(null);
    }

    /** 同步 fly_theme.is_current 标记；失败只留痕，不阻断 */
    private void markCurrent(String code) {
        if (themeDao == null) {
            return;
        }
        try {
            themeDao.clearCurrent();
            themeDao.updateCurrent(code);
        } catch (Exception e) {
            logger.warn("主题 is_current 启动同步失败（不影响启动）：code={}，原因={}", code, e.getMessage());
        }
    }
}
