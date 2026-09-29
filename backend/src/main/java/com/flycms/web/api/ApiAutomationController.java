package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.model.service.AutomationRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 自动化规则管理 REST（G17）。
 *
 * <p>事件白名单：insert/update/delete/status/comment_add/comment_audit/comment_delete（G18 契约）；
 * 动作内置 webhook/log；执行引擎见 {@link com.flycms.module.model.service.AutomationRuleService}。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiAutomationController extends ApiBaseController {

    @Autowired
    private AutomationRuleService automationRuleService;

    @ResponseBody
    @GetMapping("/system/automation/list")
    public DataVo list() {
        requirePermission("/api/system/automation/list");
        return DataVo.success("操作成功", automationRuleService.listRules());
    }

    @ResponseBody
    @PostMapping("/system/automation/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/automation/save");
        return automationRuleService.saveRule(
                parseLong(params.get("id")),
                params.get("ruleName"),
                params.get("event"),
                params.get("modelCode"),
                params.get("conditions"),
                params.get("actions"),
                parseInteger(params.get("status")));
    }

    private Long parseLong(String v) {
        try {
            return (v == null || v.isEmpty()) ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String v) {
        try {
            return (v == null || v.isEmpty()) ? null : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @ResponseBody
    @PostMapping("/system/automation/del")
    public DataVo del(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/automation/del");
        return automationRuleService.deleteRule(id);
    }
}
