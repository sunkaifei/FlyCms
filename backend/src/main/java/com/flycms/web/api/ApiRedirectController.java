package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.redirect.service.RedirectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SEO 重定向规则 REST（T-b，对标 Yoast Redirection）：
 * 管理端 CRUD，规则命中由 FlyFilter 前置 301。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/redirect")
public class ApiRedirectController extends ApiBaseController {

    @Autowired
    private RedirectService redirectService;

    /** 全部规则（来源→目标映射） */
    @GetMapping("/list")
    public DataVo list() {
        requirePermission("/api/system/redirect/list");
        return DataVo.success("操作成功", redirectService.allEnabled());
    }

    /** 保存/更新规则（source 已存在即更新） */
    @PostMapping("/save")
    public DataVo save(@RequestParam(value = "sourcePath", required = false) String sourcePath,
                       @RequestParam(value = "targetUrl", required = false) String targetUrl,
                       @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/redirect/save");
        if (StringUtils.isBlank(sourcePath) || StringUtils.isBlank(targetUrl)) {
            return DataVo.failure("来源路径与目标地址不能为空");
        }
        if (!sourcePath.startsWith("/") || sourcePath.contains("..")) {
            return DataVo.failure("来源路径必须是站内绝对路径（/ 开头）");
        }
        redirectService.save(sourcePath, targetUrl, status);
        return DataVo.success("规则已保存");
    }

    /** 删除规则 */
    @PostMapping("/delete")
    public DataVo delete(@RequestParam(value = "id", required = false) String id) {
        requirePermission("/api/system/redirect/save");
        if (StringUtils.isBlank(id)) {
            return DataVo.failure("参数传递错误");
        }
        redirectService.delete(id);
        return DataVo.success("规则已删除");
    }
}
