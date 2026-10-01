package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.preset.service.PresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站点预设 REST（P 批次零代码建站）：预设卡片/详情/幂等应用/清示例内容。
 * 权限行 900380~900382 授权超管组；应用动作幂等，可重复执行。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/preset")
public class ApiPresetController extends ApiBaseController {

    @Autowired
    private PresetService presetService;

    /** 内置预设卡片（含已应用标记） */
    @GetMapping("/list")
    public DataVo list() {
        requirePermission("/api/system/preset/list");
        return DataVo.success("操作成功", presetService.listInstalled());
    }

    /** 预设概览（将创建的对象计数） */
    @GetMapping("/detail")
    public DataVo detail(@RequestParam(value = "code", required = false) String code) {
        requirePermission("/api/system/preset/list");
        return presetService.detail(code);
    }

    /**
     * 幂等应用预设：overwriteTemplates=true 覆盖已存在模板文件（默认跳过保护管理员改动）；
     * withSampleContent=true 插入示例内容（可一键清空）。返回逐步报告。
     */
    @PostMapping("/apply")
    public DataVo apply(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/preset/list");
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            return DataVo.failure("请传入 code");
        }
        boolean overwrite = "1".equals(params.get("overwriteTemplates"));
        boolean withSample = "1".equals(params.get("withSampleContent"));
        return presetService.apply(code, overwrite, withSample, getLoginUserId());
    }

    /** 清空该预设插入的示例内容（按「【示例】」前缀） */
    @PostMapping("/clearSample")
    public DataVo clearSample(@RequestParam(value = "code", required = false) String code) {
        requirePermission("/api/system/preset/list");
        if (code == null || code.isBlank()) {
            return DataVo.failure("请传入 code");
        }
        return presetService.clearSample(code);
    }
}
