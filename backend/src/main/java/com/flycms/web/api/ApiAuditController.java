package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容审核 REST（阶段 H 引入；U3 随旧文章模块退役重写为模型驱动）。
 *
 * <p>审核对象 = 自定义模型内容（fly_cmodel_* 的 status 列：0 待审 / 1 发布 / 2 未通过）。
 * 审核开关沿用 config 键 {@code fly_article_audit}（0=直接发布，1=先审后发），
 * 键名保留既有配置值不迁移，语义已泛化为「内容投稿先审后发」。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Controller
@RequestMapping("/api")
public class ApiAuditController extends ApiBaseController {

    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ConfigService configService;

    /**
     * 待审内容分页列表（按模型）。行 = 模型动态列（id/title/shortUrl/userId/status/createTime 等）。
     *
     * @param modelId 必填——前台审核页提供模型下拉，缺省取第一个启用模型
     * @param status  不传默认 0（待审核）；0待审 1发布 2未通过，传 -1 查全部
     */
    @ResponseBody
    @GetMapping("/system/audit/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "modelId", required = false) Long modelId,
                       @RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "status", defaultValue = "0") Integer status) {
        requirePermission("/api/system/audit/page");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        if (mid == null) {
            return DataVo.failure("没有启用的内容模型");
        }
        Integer real = (status != null && status < 0) ? null : status;
        PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
                mid, title, null, real, new HashMap<>(), "id", "desc",
                pageNum, rows, null, false);
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 跨模型聚合分页（审核队列默认视图）。
     *
     * <p>此前审核页必须先选一个模型才能看到内容——投稿分散在多个模型时，
     * 审核员要逐个模型切着看，漏审是常态。这里按模型归并后统一排序分页。
     *
     * <p>复杂度控制：先取各模型命中数，只对命中数 &gt; 0 的模型取前 (offset+rows) 行，
     * 空模型一次查询都不发。
     */
    @ResponseBody
    @GetMapping("/system/audit/pageAll")
    public DataVo pageAll(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                          @RequestParam(value = "rows", defaultValue = "20") int rows,
                          @RequestParam(value = "title", required = false) String title,
                          @RequestParam(value = "status", defaultValue = "0") Integer status) {
        requirePermission("/api/system/audit/pageAll");
        Integer real = (status != null && status < 0) ? null : status;
        List<Model> list = enabledModels();
        PageVo<Map<String, Object>> pageVo = new PageVo<>(pageNum);
        if (list.isEmpty()) {
            pageVo.setList(new ArrayList<>());
            pageVo.setCount(0);
            return DataVo.success("操作成功", pageVo);
        }
        Map<String, Object> merged =
                modelDataService.mergeAuditPage(list, title, real, pageNum, rows);
        pageVo.setList((List<Map<String, Object>>) merged.get("list"));
        pageVo.setCount(((Number) merged.get("count")).intValue());
        return DataVo.success("操作成功", pageVo);
    }

    /** 待审数量（角标）：全部启用模型 status=0 之和 */
    @ResponseBody
    @GetMapping("/system/audit/pendingCount")
    public DataVo pendingCount() {
        requirePermission("/api/system/audit/page");
        int total = 0;
        for (Model m : enabledModels()) {
            try {
                total += modelDataService.countByStatus(m.getId(), 0);
            } catch (Exception ignored) {
                // 单个模型表缺失（未生成）跳过
            }
        }
        return DataVo.success("操作成功", total);
    }

    /**
     * 单条审核：status 1=通过（发布） 2=驳回（未通过）
     *
     * <p><b>2026-10-23 修复</b>：此前缺少 reason 形参，前端传来的驳回原因被 Spring
     * 静默丢弃（只按形参绑定），管理员现场填写的理由一条都留不下来。这里接住并落
     * {@code fly_content_audit_log}。
     */
    @ResponseBody
    @PostMapping("/system/audit/audit")
    public DataVo audit(@RequestParam(value = "modelId", required = false) Long modelId,
                        @RequestParam(value = "id", defaultValue = "0") Long id,
                        @RequestParam(value = "status", defaultValue = "1") Integer status,
                        @RequestParam(value = "reason", required = false) String reason,
                        @RequestParam(value = "notify", defaultValue = "1") Integer notify) {
        requirePermission("/api/system/audit/audit");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        if (mid == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        if (target == 2 && StringUtils.isBlank(reason)) {
            return DataVo.failure("驳回必须填写原因");
        }
        Admin admin = requireAdmin();
        return modelDataService.updateStatus(mid, List.of(id), target,
                reason, admin.getId(), admin.getAdminName(), Boolean.TRUE,
                notifyEnabled(notify));
    }

    /**
     * 批量审核：status 1=通过 2=驳回，ids 支持 "1,2,3"
     *
     * <p>批量驳回共用同一条原因（一次决定覆盖多条内容），落库后每条内容各有一行日志。
     */
    @ResponseBody
    @PostMapping("/system/audit/batch")
    public DataVo batch(@RequestParam(value = "modelId", required = false) Long modelId,
                        @RequestParam(value = "ids", defaultValue = "") String ids,
                        @RequestParam(value = "status", defaultValue = "1") Integer status,
                        @RequestParam(value = "reason", required = false) String reason,
                        @RequestParam(value = "notify", defaultValue = "1") Integer notify) {
        requirePermission("/api/system/audit/batch");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        List<Long> idList = parseIds(ids);
        if (mid == null || idList.isEmpty()) {
            return DataVo.failure("参数传递错误");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        if (target == 2 && StringUtils.isBlank(reason)) {
            return DataVo.failure("驳回必须填写原因");
        }
        Admin admin = requireAdmin();
        return modelDataService.updateStatus(mid, idList, target,
                reason, admin.getId(), admin.getAdminName(), Boolean.TRUE,
                notifyEnabled(notify));
    }

    /**
     * notify 开关求值。
     *
     * <p>曾经写成 {@code Boolean.TRUE.equals(notify == null ? 1 : notify)} —— 
     * Boolean.equals(Integer) 恒为 false，于是「通知作者」这个开关被悄悄关掉，
     * 而接口一切正常、回执也只看得出"未发送"。类型不匹配的静默失败，
     * 只能靠端到端探针抓出来。
     */
    private boolean notifyEnabled(Integer notify) {
        return notify == null || notify != 0;
    }

    /**
     * 单条内容详情（供审核页「查看内容」：必须能看到正文才能做决定，
     * 此前审核列表只有标题，审核员只能对着标题点通过/驳回）。
     *
     * @return 整行（含 content 与全部自定义字段）
     */
    @ResponseBody
    @GetMapping("/system/audit/detail")
    public DataVo detail(@RequestParam(value = "modelId", required = false) Long modelId,
                         @RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/audit/detail");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        if (mid == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        Map<String, Object> row = modelDataService.findDataById(mid, id);
        if (row == null) {
            return DataVo.failure("内容不存在或已删除");
        }
        return DataVo.success("操作成功", row);
    }

    /**
     * 某条内容的审核历史（驳回原因可追溯）。
     */
    @ResponseBody
    @GetMapping("/system/audit/logs")
    public DataVo logs(@RequestParam(value = "modelId", required = false) Long modelId,
                       @RequestParam(value = "id", defaultValue = "0") Long id,
                       @RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows) {
        requirePermission("/api/system/audit/logs");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        if (mid == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        return DataVo.success("操作成功",
                modelDataService.selectAuditLogs(mid, id, pageNum, rows));
    }

    /** 读取投稿审核开关 */
    @ResponseBody
    @GetMapping("/system/audit/switch")
    public DataVo auditSwitch() {
        requirePermission("/api/system/audit/switch");
        return DataVo.success("操作成功", getSwitchValue());
    }

    /** 设置投稿审核开关：0=直接发布 1=先审后发 */
    @ResponseBody
    @PostMapping("/system/audit/switch")
    public DataVo setSwitch(@RequestParam(value = "value", defaultValue = "0") Integer value) {
        requirePermission("/api/system/audit/switch");
        int v = (value != null && value == 1) ? 1 : 0;
        configService.updagteConfigByKey("fly_article_audit", String.valueOf(v));
        return DataVo.success("已保存");
    }

    private int getSwitchValue() {
        String value = configService.getStringByKey("fly_article_audit");
        return "1".equals(StringUtils.trimToEmpty(value)) ? 1 : 0;
    }

    private List<Model> enabledModels() {
        List<Model> list = modelService.getEnabledModels();
        return list == null ? new ArrayList<>() : list;
    }

    private Long firstEnabledModelId() {
        List<Model> list = enabledModels();
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private List<Long> parseIds(String ids) {
        List<Long> list = new ArrayList<Long>();
        if (StringUtils.isBlank(ids)) {
            return list;
        }
        for (String part : ids.split("[,;\\s]+")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            try {
                list.add(Long.parseLong(v));
            } catch (NumberFormatException e) {
                // 忽略非法 id
            }
        }
        return list;
    }
}
