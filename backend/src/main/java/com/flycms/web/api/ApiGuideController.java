package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.guide.model.Guide;
import com.flycms.module.guide.service.GuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点导航 REST 接口（vben 后台，fly_guide）。
 *
 * 权限码 = action_key 原样（种子节点 900240~900245，/api/** 不经 permission_sync）；
 * 未登录由 requireAdmin 抛 HTTP 401，无权限 requirePermission 抛 403。
 * 前端表单 POST 统一 form-urlencoded（@RequestParam 读取）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/guide")
public class ApiGuideController extends ApiBaseController {

    @Autowired
    private GuideService guideService;

    /** 导航项分页列表（平铺，后台列表页；father_id+sort 排序） */
    @GetMapping("/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "50") int rows) {
        requirePermission("/api/system/guide/page");
        PageVo<Guide> pageVo = guideService.findPage(pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /** 导航树（含隐藏项，父项选择/树形表格用），节点带计算后的 url */
    @GetMapping("/tree")
    public DataVo tree() {
        requirePermission("/api/system/guide/tree");
        return DataVo.success("操作成功", guideService.tree(false));
    }

    /** 导航项详情（带计算后的 url） */
    @GetMapping("/get")
    public DataVo get(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/guide/get");
        Guide guide = guideService.findById(id);
        if (guide == null) {
            return DataVo.failure("导航项不存在");
        }
        guide.setUrl(guideService.resolveUrl(guide));
        return DataVo.success("操作成功", guide);
    }

    /** 新增/修改导航项（id 空=新增）：name/fatherId/type/refId/link/target/sort/status */
    @PostMapping("/save")
    public DataVo save(@RequestParam(value = "id", required = false) Long id,
                       @RequestParam(value = "fatherId", defaultValue = "0") Long fatherId,
                       @RequestParam(value = "name", required = false) String name,
                       @RequestParam(value = "type", defaultValue = "0") int type,
                       @RequestParam(value = "refId", required = false) Long refId,
                       @RequestParam(value = "link", required = false) String link,
                       @RequestParam(value = "target", required = false) String target,
                       @RequestParam(value = "sort", defaultValue = "0") int sort,
                       @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/guide/save");
        Guide form = new Guide();
        form.setId(id);
        form.setFatherId(fatherId);
        form.setName(name);
        form.setType(type);
        form.setRefId(refId);
        form.setLink(link);
        form.setTarget(target);
        form.setSort(sort);
        form.setStatus(status);
        return guideService.save(form);
    }

    /** 删除导航项（有下级时拒绝） */
    @PostMapping("/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/guide/delete");
        return guideService.delete(id);
    }

    /** 显隐切换（status：1 显示 / 0 隐藏） */
    @PostMapping("/status")
    public DataVo status(@RequestParam(value = "id", defaultValue = "0") Long id,
                         @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/guide/status");
        return guideService.updateStatus(id, status);
    }
}
