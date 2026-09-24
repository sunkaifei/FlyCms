package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.links.model.Links;
import com.flycms.module.links.service.LinksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 友情链接管理 REST（规划阶段 B4）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiLinksController extends ApiBaseController {

    @Autowired
    private LinksService linksService;

    /**
     * 友链分页列表
     *
     * @param type   链接类型，不传=全部
     * @param isShow 显示状态 1显示 0不显示，不传=全部
     */
    @ResponseBody
    @GetMapping("/system/links/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "type", required = false) Integer type,
                       @RequestParam(value = "isShow", required = false) Integer isShow,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/links/page");
        PageVo<Links> pageVo = linksService.getLinksLibraryPage(type, isShow, keyword, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/links/get")
    public DataVo get(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/links/page");
        Links links = linksService.findLinksById(id);
        if (links == null) {
            return DataVo.failure("友链不存在");
        }
        return DataVo.success("操作成功", links);
    }

    /**
     * 新增/更新友链
     */
    @ResponseBody
    @PostMapping("/system/links/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/links/save");
        Links links = new Links();
        String id = params.get("id");
        if (id != null && !id.trim().isEmpty()) {
            try {
                links.setId(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                return DataVo.failure("id 不合法");
            }
        }
        links.setType(parseInt(params.get("type"), 0));
        links.setLinkName(params.get("linkName"));
        links.setLinkUrl(params.get("linkUrl"));
        links.setLinkLogo(params.get("linkLogo"));
        links.setIsShow(parseInt(params.get("isShow"), 1));
        links.setSort(parseInt(params.get("sort"), 0));
        if (links.getLinkName() == null || links.getLinkName().trim().isEmpty()) {
            return DataVo.failure("请填写网站名称");
        }
        if (links.getLinkUrl() == null || links.getLinkUrl().trim().isEmpty()) {
            return DataVo.failure("请填写网站网址");
        }
        links.setCreateTime(new Date());
        return linksService.saveLinks(links);
    }

    /**
     * 删除友链
     */
    @ResponseBody
    @PostMapping("/system/links/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/links/delete");
        boolean ok = linksService.deleteLinksById(id);
        return ok ? DataVo.success("删除成功") : DataVo.failure("友链不存在");
    }

    /**
     * 切换显示状态
     */
    @ResponseBody
    @PostMapping("/system/links/status")
    public DataVo status(@RequestParam(value = "id", defaultValue = "0") Long id,
                         @RequestParam(value = "isShow", defaultValue = "1") Integer isShow) {
        requirePermission("/api/system/links/save");
        return linksService.updateLinksStatus(id, isShow);
    }

    private int parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
