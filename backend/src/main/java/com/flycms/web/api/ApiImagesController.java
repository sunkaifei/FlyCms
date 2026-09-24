package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.question.model.Images;
import com.flycms.module.question.service.ImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附件库管理 REST（规划阶段 B2）
 *
 * fly_images 已有 info_count 引用计数，这里补统一浏览 + 孤儿清理：
 * 孤儿 = 引用计数为 0（或已标记 img_delete=1），清理只删这类记录，不会误删在用附件。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiImagesController extends ApiBaseController {

    @Autowired
    private ImagesService imagesService;

    /**
     * 附件分页列表
     *
     * @param onlyOrphan 1=只看孤儿
     */
    @ResponseBody
    @GetMapping("/system/images/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "onlyOrphan", defaultValue = "0") Integer onlyOrphan) {
        requirePermission("/api/system/images/page");
        PageVo<Images> pageVo = imagesService.getImagesLibraryPage(keyword, onlyOrphan != null && onlyOrphan == 1,
                pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 孤儿附件统计
     */
    @ResponseBody
    @GetMapping("/system/images/orphanCount")
    public DataVo orphanCount() {
        requirePermission("/api/system/images/page");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("count", imagesService.countOrphanImages());
        return DataVo.success("操作成功", data);
    }

    /**
     * 清理孤儿附件。ids 为空表示清理全部孤儿。
     */
    @ResponseBody
    @PostMapping("/system/images/deleteOrphan")
    public DataVo deleteOrphan(@RequestParam(value = "ids", defaultValue = "") String ids) {
        requirePermission("/api/system/images/deleteOrphan");
        int rows = imagesService.deleteOrphanImages(parseIds(ids));
        return DataVo.success("已清理 " + rows + " 个孤儿附件");
    }

    private List<Long> parseIds(String ids) {
        List<Long> list = new ArrayList<Long>();
        if (ids == null || ids.trim().isEmpty()) {
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
