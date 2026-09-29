package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
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
     */
    @ResponseBody
    @PostMapping("/system/audit/audit")
    public DataVo audit(@RequestParam(value = "modelId", required = false) Long modelId,
                        @RequestParam(value = "id", defaultValue = "0") Long id,
                        @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/audit/audit");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        if (mid == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        return modelDataService.updateStatus(mid, List.of(id), target);
    }

    /**
     * 批量审核：status 1=通过 2=驳回，ids 支持 "1,2,3"
     */
    @ResponseBody
    @PostMapping("/system/audit/batch")
    public DataVo batch(@RequestParam(value = "modelId", required = false) Long modelId,
                        @RequestParam(value = "ids", defaultValue = "") String ids,
                        @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/audit/batch");
        Long mid = modelId != null && modelId > 0 ? modelId : firstEnabledModelId();
        List<Long> idList = parseIds(ids);
        if (mid == null || idList.isEmpty()) {
            return DataVo.failure("参数传递错误");
        }
        int target = (status != null && status == 2) ? 2 : 1;
        return modelDataService.updateStatus(mid, idList, target);
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
