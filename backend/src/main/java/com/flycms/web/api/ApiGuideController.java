package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.model.Guide;
import com.flycms.module.config.service.GuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导航管理 REST（规划阶段 B3）
 *
 * 存量 fly_guide 为平表（无父级列），对外提供排序后的扁平列表，
 * 前端按 sort 渲染；接口保留 tree 语义入口（等价于 list）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiGuideController extends ApiBaseController {

    @Autowired
    private GuideService guideService;

    /**
     * 导航列表（不分页，按 sort 升序）
     */
    @ResponseBody
    @GetMapping("/system/guide/tree")
    public DataVo tree(@RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/guide/tree");
        List<Guide> list = guideService.getGuideAll(status);
        return DataVo.success("操作成功", list);
    }

    /**
     * 导航分页列表（支持名称模糊搜索）
     */
    @ResponseBody
    @GetMapping("/system/guide/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "name", required = false) String name,
                       @RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/guide/tree");
        PageVo<Guide> pageVo = guideService.getGuideListPage(name, status, "sort", "asc", pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 导航详情
     */
    @ResponseBody
    @GetMapping("/system/guide/get")
    public DataVo get(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/guide/tree");
        Guide guide = guideService.findGuideById(id);
        if (guide == null) {
            return DataVo.failure("导航不存在");
        }
        return DataVo.success("操作成功", guide);
    }

    /**
     * 新增/更新导航
     */
    @ResponseBody
    @PostMapping("/system/guide/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/guide/save");
        Guide guide = new Guide();
        String id = params.get("id");
        if (id != null && !id.trim().isEmpty()) {
            try {
                guide.setId(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                return DataVo.failure("id 不合法");
            }
        }
        guide.setName(params.get("name"));
        guide.setLink(params.get("link"));
        guide.setSort(parseInt(params.get("sort"), 0));
        guide.setStatus(parseInt(params.get("status"), 1));
        if (guide.getName() == null || guide.getName().trim().isEmpty()) {
            return DataVo.failure("请填写导航名称");
        }
        return guideService.saveGuide(guide);
    }

    /**
     * 删除导航
     */
    @ResponseBody
    @PostMapping("/system/guide/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/guide/delete");
        return guideService.deleteGuide(id);
    }

    /**
     * 切换显示状态
     */
    @ResponseBody
    @PostMapping("/system/guide/status")
    public DataVo status(@RequestParam(value = "id", defaultValue = "0") Long id,
                         @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/guide/save");
        return guideService.updateGuideStatus(id, status);
    }

    private int parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
