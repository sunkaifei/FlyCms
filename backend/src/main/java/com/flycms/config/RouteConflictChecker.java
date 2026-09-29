package com.flycms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * G27 根级路由冲突自检（启动时一次，不阻断启动）。
 *
 * <p>背景：本站根级单段/两段通配路由并存（/{modelCode}/、/{channelDir}/、/ac/{id}…），
 * 历史上 ChannelController 曾因 pattern 同形导致 Ambiguous mapping 退服；Spring 对
 * 字面量优先的排序让多数同形 pattern "侥幸可用"，但新增根级映射极易再踩。
 * 本检查在启动日志里把「全段都是 {var} 的同形 pattern」按段数分组显式报警：
 * 同段数出现 ≥ 2 个 handler 即 log.error 列出，第一时间暴露风险面。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
public class RouteConflictChecker {

    private static final Logger log = LoggerFactory.getLogger(RouteConflictChecker.class);

    @EventListener(ContextRefreshedEvent.class)
    public void check(ContextRefreshedEvent event) {
        try {
            ApplicationContext ctx = event.getApplicationContext();
            RequestMappingHandlerMapping mapping = ctx.getBean(RequestMappingHandlerMapping.class);
            Map<Integer, List<String>> bySegments = new LinkedHashMap<>();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
                var pc = e.getKey().getPathPatternsCondition();
                if (pc == null) {
                    continue;
                }
                for (var p : pc.getPatterns()) {
                    String text = p.getPatternString();
                    List<String> vars = new ArrayList<>();
                    boolean allVar = true;
                    for (String seg : text.split("/")) {
                        if (seg.isEmpty()) {
                            continue;
                        }
                        if (seg.startsWith("{")) {
                            vars.add(seg);
                        } else {
                            allVar = false;
                            break;
                        }
                    }
                    if (allVar && !vars.isEmpty()) {
                        bySegments.computeIfAbsent(vars.size(), k -> new ArrayList<>())
                                .add(text + "  →  " + e.getValue().getBeanType().getSimpleName()
                                        + "#" + e.getValue().getMethod().getName());
                    }
                }
            }
            for (Map.Entry<Integer, List<String>> e : bySegments.entrySet()) {
                if (e.getValue().size() > 1) {
                    log.error("[G27 路由冲突面] 存在 {} 个同形（{} 段全变量）根级路由，当前依赖 Spring 精确度排序"
                                    + "侥幸可用，新增根级映射前务必核对：{}",
                            e.getValue().size(), e.getKey(), e.getValue());
                }
            }
        } catch (Exception e) {
            log.warn("[G27] 路由自检失败（不影响启动）：{}", e.getMessage());
        }
    }
}
