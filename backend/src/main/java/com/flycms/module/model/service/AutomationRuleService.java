package com.flycms.module.model.service;

import com.alibaba.fastjson2.JSON;
import com.flycms.core.entity.DataVo;
import com.flycms.core.event.ContentChangedEvent;
import com.flycms.core.utils.SnowFlake;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.AutomationRuleDao;
import com.flycms.module.model.model.AutomationRule;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化规则服务（G17，Directus Flows 简化版）。
 *
 * <p>监听 G18 的 {@link ContentChangedEvent}：event + model_code 匹配启用规则 →
 * conditions（可选，对首行内容求值）→ 依次执行内置动作。内置动作集：
 * <ul>
 *   <li>{@code webhook}：POST JSON（event/modelCode/ids/ruleName）到 url，5s 超时；</li>
 *   <li>{@code log}：写 INFO 日志（最简可见动作）。</li>
 * </ul>
 * 动作逐个 try/catch——单个动作失败不影响其余动作与主流程。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class AutomationRuleService {

    private static final Logger log = LoggerFactory.getLogger(AutomationRuleService.class);

    /** 事件白名单（与 ContentChangedEvent 的 action 对齐） */
    private static final List<String> EVENTS = List.of(
            "insert", "update", "delete", "status",
            "comment_add", "comment_audit", "comment_delete");

    @Autowired
    private AutomationRuleDao automationRuleDao;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelService modelService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    // /////////////////// 管理端 ///////////////////

    public List<Map<String, Object>> listRules() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AutomationRule r : automationRuleDao.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(r.getId()));
            m.put("ruleName", r.getRuleName());
            m.put("event", r.getEvent());
            m.put("modelCode", r.getModelCode());
            m.put("conditions", r.getConditions());
            m.put("actions", r.getActions());
            m.put("status", r.getStatus());
            m.put("createTime", String.valueOf(r.getCreateTime()));
            out.add(m);
        }
        return out;
    }

    public DataVo saveRule(Long id, String ruleName, String event, String modelCode,
                           String conditions, String actions, Integer status) {
        if (StringUtils.isBlank(ruleName) || StringUtils.isBlank(event)) {
            return DataVo.failure("规则名称与事件不能为空");
        }
        if (!EVENTS.contains(event)) {
            return DataVo.failure("不支持的事件：" + event);
        }
        if (StringUtils.isNotBlank(modelCode)) {
            try {
                SqlSafeUtil.safeModelCode(modelCode);
            } catch (IllegalArgumentException e) {
                return DataVo.failure("模型标识不合法");
            }
            if (modelService.findModelByCode(modelCode) == null) {
                return DataVo.failure("限定模型不存在");
            }
        }
        if (StringUtils.isBlank(actions)) {
            return DataVo.failure("至少配置一个动作");
        }
        List<Map<String, Object>> actionList = parseList(actions);
        for (Map<String, Object> a : actionList) {
            String type = String.valueOf(a.get("type"));
            if ("webhook".equals(type) && StringUtils.isBlank((String) a.get("url"))) {
                return DataVo.failure("webhook 动作必须配置 url");
            }
            if (!"webhook".equals(type) && !"log".equals(type)) {
                return DataVo.failure("不支持的动作类型：" + type);
            }
        }
        AutomationRule rule = new AutomationRule();
        rule.setRuleName(ruleName.trim());
        rule.setEvent(event);
        rule.setModelCode(StringUtils.trimToNull(modelCode));
        rule.setConditions(StringUtils.trimToNull(conditions));
        rule.setActions(actions);
        rule.setStatus(status != null && status == 1 ? 1 : 0);
        if (id != null && id > 0) {
            if (automationRuleDao.findById(id) == null) {
                return DataVo.failure("规则不存在");
            }
            rule.setId(id);
            automationRuleDao.update(rule);
            return DataVo.success("规则已更新");
        }
        rule.setId(SnowFlake.getInstance().nextId());
        automationRuleDao.add(rule);
        return DataVo.success("规则已创建");
    }

    public DataVo deleteRule(Long id) {
        automationRuleDao.delete(id);
        return DataVo.success("已删除");
    }

    // /////////////////// 事件消费（G18 契约） ///////////////////

    @org.springframework.context.event.EventListener
    public void onContentChanged(ContentChangedEvent event) {
        try {
            List<AutomationRule> rules =
                    automationRuleDao.findEnabledByEvent(event.getAction());
            if (rules.isEmpty()) {
                return;
            }
            for (AutomationRule rule : rules) {
                if (StringUtils.isNotBlank(rule.getModelCode())
                        && !rule.getModelCode().equals(event.getModelCode())) {
                    continue;
                }
                execute(rule, event);
            }
        } catch (Exception e) {
            // 事件消费异常绝不阻断主流程
            log.warn("[G17] 自动化规则执行异常：{}", e.getMessage());
        }
    }

    private void execute(AutomationRule rule, ContentChangedEvent event) {
        // 全方法 try/catch 包裹（webhook 的 IOException 等不外泄到事件总线）
        // conditions：对首行内容求值（可选；不满足即整条规则跳过）
        if (StringUtils.isNotBlank(rule.getConditions())
                && !event.getIds().isEmpty() && !matchConditions(rule, event)) {
            return;
        }
        List<Map<String, Object>> actions = parseList(rule.getActions());
        for (Map<String, Object> a : actions) {
            try {
                String type = String.valueOf(a.get("type"));
                if ("webhook".equals(type)) {
                    fireWebhook(rule, event, String.valueOf(a.get("url")));
                } else if ("log".equals(type)) {
                    log.info("[G17 自动化] 规则「{}」命中事件 {}（模型 {}，ids {}）",
                            rule.getRuleName(), event.getAction(), event.getModelCode(), event.getIds());
                }
            } catch (Exception e) {
                log.warn("[G17 自动化] 规则「{}」动作执行失败：{}", rule.getRuleName(), e.getMessage());
            }
        }
    }

    /** conditions 对首行内容求值：eq/neq/gt/lt/contains（字符串弱比较，gt/lt 数字优先） */
    private boolean matchConditions(AutomationRule rule, ContentChangedEvent event) {
        try {
            Long modelId = resolveModelId(event.getModelCode());
            if (modelId == null) {
                return false;
            }
            Map<String, Object> row = modelDataService.findDataById(modelId, event.getIds().get(0));
            if (row == null) {
                return false;
            }
            List<Map<String, Object>> conds = parseList(rule.getConditions());
            for (Map<String, Object> c : conds) {
                String field = String.valueOf(c.get("field"));
                String op = String.valueOf(c.get("op"));
                Object expected = c.get("value");
                Object actual = normalizeKey(row, field);
                String a = actual == null ? "" : String.valueOf(actual);
                String b = expected == null ? "" : String.valueOf(expected);
                boolean hit;
                switch (op) {
                    case "neq" -> hit = !a.equals(b);
                    case "gt" -> hit = compare(a, b) > 0;
                    case "lt" -> hit = compare(a, b) < 0;
                    case "contains" -> hit = a.contains(b);
                    default -> hit = a.equals(b);
                }
                if (!hit) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("[G17 自动化] 条件求值失败（规则「{}」）：{}", rule.getRuleName(), e.getMessage());
            return false;
        }
    }

    private Object normalizeKey(Map<String, Object> row, String field) {
        if (row.containsKey(field)) {
            return row.get(field);
        }
        // 驼峰别名（normalizeKeys 同款转换）
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char ch : field.toCharArray()) {
            if (ch == '_') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(ch) : ch);
                up = false;
            }
        }
        return row.get(sb.toString());
    }

    private int compare(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    private Long resolveModelId(String modelCode) {
        var model = modelService.findModelByCode(modelCode);
        return model == null ? null : model.getId();
    }

    private void fireWebhook(AutomationRule rule, ContentChangedEvent event, String url) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event.getAction());
        payload.put("modelCode", event.getModelCode());
        payload.put("ids", event.getIds());
        payload.put("operatorId", event.getOperatorId());
        payload.put("rule", rule.getRuleName());
        payload.put("time", String.valueOf(event.getTime()));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-FlyCms-Event", event.getAction())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(payload)))
                .build();
        HttpResponse<String> resp =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[G17 自动化] webhook「{}」→ {} 响应 {}", rule.getRuleName(), url, resp.statusCode());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(String json) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (StringUtils.isBlank(json)) {
            return out;
        }
        try {
            List<Object> arr = JSON.parseArray(json);
            if (arr != null) {
                for (Object o : arr) {
                    if (o instanceof Map) {
                        out.add((Map<String, Object>) o);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[G17 自动化] JSON 解析失败：{}", e.getMessage());
        }
        return out;
    }
}
