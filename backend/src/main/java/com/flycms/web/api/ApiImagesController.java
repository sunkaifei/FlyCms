package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.images.model.Images;
import com.flycms.module.images.service.ImagesService;
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

    /**
     * 管理端图片直传（W 批次字段控件层前置）：字段表单/画廊控件内直接上传，
     * 落 fly_images（info_count=0 孤儿态，被内容引用后由引用计数接管）。
     * 安全沿用 UploadSafeUtil 三重白名单（扩展名/MIME/魔数）+ 强制重命名，上限 2MB。
     */
    @ResponseBody
    @PostMapping("/system/images/upload")
    public DataVo upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        requirePermission("/api/system/images/page");
        if (file == null || file.isEmpty()) {
            return DataVo.failure("请选择图片");
        }
        try {
            com.flycms.core.utils.UploadSafeUtil.ImageType type =
                    com.flycms.core.utils.UploadSafeUtil.safeImage(file, com.flycms.core.utils.UploadSafeUtil.MAX_IMAGE_BYTES);
            if (type == null) {
                return DataVo.failure("文件不是合法图片（仅支持 .jpg/.png/.gif/.bmp/.webp，且不得超过 2MB）");
            }
            String dateDir = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            String path = com.flycms.constant.Const.UPLOAD_PATH + "/upload/admin/" + dateDir + "/";
            String fileName = com.flycms.core.utils.UploadSafeUtil.rename(type);
            java.io.File dest = new java.io.File(path + fileName);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                return DataVo.failure("上传目录创建失败");
            }
            imagesService.uploadFile(file.getBytes(), path, fileName);
            java.awt.image.BufferedImage source = javax.imageio.ImageIO.read(dest);
            Images images = new Images();
            images.setId(com.flycms.core.utils.SnowFlake.getInstance().nextId());
            images.setImgUrl("/upload/admin/" + dateDir + "/" + fileName);
            images.setImgName(file.getOriginalFilename());
            images.setFileSize(String.format("%.1f", dest.length() / 1024.0));
            if (source != null) {
                images.setImgWidth(Integer.toString(source.getWidth()));
                images.setImgHeight(Integer.toString(source.getHeight()));
            }
            images.setSort(0);
            images.setCreateTime(new java.util.Date());
            images.setInfoCount(0);
            imagesService.addAdminUpload(images);
            Map<String, Object> data = new HashMap<>();
            data.put("id", String.valueOf(images.getId()));
            data.put("imgUrl", images.getImgUrl());
            data.put("imgName", images.getImgName());
            return DataVo.success("上传成功", data);
        } catch (Exception e) {
            return DataVo.failure("上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量 id → url/name 映射（W 批次控件回显）：编辑表单打开时一次拉取，
     * 修复「刷新后缩略图退化为 ID 占位」。上限 100 个。
     */
    @ResponseBody
    @GetMapping("/system/images/batch")
    public DataVo batch(@RequestParam(value = "ids", defaultValue = "") String ids) {
        requireAdmin();
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return DataVo.failure("请传入 ids");
        }
        if (idList.size() > 100) {
            return DataVo.failure("单次最多 100 个 id");
        }
        List<Map<String, Object>> list = imagesService.findByIds(idList);
        return DataVo.success("操作成功", list);
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
