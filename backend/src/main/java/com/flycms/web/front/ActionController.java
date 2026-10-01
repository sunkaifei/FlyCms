package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.model.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ② 动作原语前台端点（W/Z 批次动作层）：登录用户对任意启用模型的运行时写动作。
 * 与投稿通道（/ucenter/submit）的分工：submit=内容投稿（走审核开关），action=互动
 * 记录（购物车/报名/计数，行状态不走审核开关）。权限行 /ucenter/action/* 已授权全部用户组。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/ucenter/action")
public class ActionController extends BaseController {

    @Autowired
    private ActionService actionService;

    /** 单行/批量动作写入：body 表单（单行）或 rows=JSON 数组（批量） */
    @PostMapping("/{modelCode}")
    public DataVo submit(@PathVariable String modelCode,
                         @RequestParam Map<String, String> form) {
        if (getUser() == null) {
            return DataVo.failure("请登录后操作");
        }
        String rows = form.get("rows");
        if (rows != null && !rows.isBlank()) {
            return actionService.submitBatch(modelCode, rows, getUser().getUserId());
        }
        return actionService.submit(modelCode, form, getUser().getUserId());
    }

    /** 字段原子增减（GET 便于模板表单提交，delta 支持负数） */
    @GetMapping("/{modelCode}/{id}/counter")
    public DataVo counterGet(@PathVariable String modelCode,
                             @PathVariable Long id,
                             @RequestParam(value = "field", required = false) String field,
                             @RequestParam(value = "delta", required = false) String delta) {
        return counterPost(modelCode, id, field, delta);
    }

    @PostMapping("/{modelCode}/{id}/counter")
    public DataVo counterPost(@PathVariable String modelCode,
                              @PathVariable Long id,
                              @RequestParam(value = "field", required = false) String field,
                              @RequestParam(value = "delta", required = false) String delta) {
        if (getUser() == null) {
            return DataVo.failure("请登录后操作");
        }
        if (field == null || field.isBlank()) {
            return DataVo.failure("请传入 field");
        }
        BigDecimal d;
        try {
            d = new BigDecimal(delta == null ? "" : delta.trim());
        } catch (NumberFormatException e) {
            return DataVo.failure("delta 不是合法数字");
        }
        return actionService.counter(modelCode, id, field, d, getUser().getUserId());
    }

    /** 组合事务：actions=JSON 数组（[{op:insert,model,fields}, {op:counter,model,id,field,delta}]） */
    @PostMapping("/compose")
    public DataVo compose(@RequestParam(value = "actions", required = false) String actions) {
        if (getUser() == null) {
            return DataVo.failure("请登录后操作");
        }
        if (actions == null || actions.isBlank()) {
            return DataVo.failure("请传入 actions");
        }
        return actionService.compose(actions, getUser().getUserId());
    }
}
