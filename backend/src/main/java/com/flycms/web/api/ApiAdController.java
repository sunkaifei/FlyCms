package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.ad.model.Ad;
import com.flycms.module.ad.model.AdPosition;
import com.flycms.module.ad.service.AdService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 广告系统 REST 接口（vben 后台，fly_ad_position / fly_ad）。
 *
 * 权限码 = action_key 原样（/api/system/ad/**，见 sql/migrations/2026-09-30-ad.sql）；
 * 未登录由 requireAdmin 抛 HTTP 401，无权限 requirePermission 抛 403。
 * 前端表单 POST 统一 form-urlencoded（@RequestParam 读取）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/ad")
public class ApiAdController extends ApiBaseController {

    @Autowired
    private AdService adService;

    // /////////////////// 广告位 ///////////////////

    /** 广告位分页列表（keyword 模糊匹配名称/标识） */
    @ResponseBody
    @GetMapping("/position/list")
    public DataVo positionList(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                               @RequestParam(value = "rows", defaultValue = "20") int rows,
                               @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/ad/position/list");
        PageVo<AdPosition> pageVo = adService.findPositionPage(pageNum, rows, keyword);
        return DataVo.success("操作成功", pageVo);
    }

    /** 新增广告位（name/adKey/description/width/height/sort/status） */
    @ResponseBody
    @PostMapping("/position/save")
    public DataVo positionSave(@RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "adKey", required = false) String adKey,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "width", required = false) Integer width,
                               @RequestParam(value = "height", required = false) Integer height,
                               @RequestParam(value = "sort", defaultValue = "0") int sort,
                               @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/ad/position/save");
        AdPosition form = new AdPosition();
        form.setName(name);
        form.setAdKey(adKey);
        form.setDescription(description);
        form.setWidth(width);
        form.setHeight(height);
        form.setSort(sort);
        form.setStatus(status);
        return adService.addPosition(form);
    }

    /** 编辑广告位（id 必传；adKey 创建后不可改） */
    @ResponseBody
    @PostMapping("/position/update")
    public DataVo positionUpdate(@RequestParam(value = "id", required = false) Long id,
                                 @RequestParam(value = "name", required = false) String name,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "width", required = false) Integer width,
                                 @RequestParam(value = "height", required = false) Integer height,
                                 @RequestParam(value = "sort", defaultValue = "0") int sort,
                                 @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/ad/position/update");
        AdPosition form = new AdPosition();
        form.setId(id);
        form.setName(name);
        form.setDescription(description);
        form.setWidth(width);
        form.setHeight(height);
        form.setSort(sort);
        form.setStatus(status);
        return adService.updatePosition(form);
    }

    /** 删除广告位（位下有广告时拒绝） */
    @ResponseBody
    @PostMapping("/position/delete")
    public DataVo positionDelete(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/ad/position/delete");
        if (id == null || id <= 0) {
            return DataVo.failure("广告位 id 不能为空");
        }
        return adService.deletePosition(id);
    }

    // /////////////////// 广告 ///////////////////

    /** 广告分页列表（positionId 必传） */
    @ResponseBody
    @GetMapping("/ad/list")
    public DataVo adList(@RequestParam(value = "positionId", required = false) Long positionId,
                         @RequestParam(value = "p", defaultValue = "1") int pageNum,
                         @RequestParam(value = "rows", defaultValue = "20") int rows,
                         @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/ad/ad/list");
        if (positionId == null) {
            return DataVo.failure("positionId 不能为空");
        }
        PageVo<Ad> pageVo = adService.findAdPage(pageNum, rows, positionId, keyword);
        return DataVo.success("操作成功", pageVo);
    }

    /** 新增广告（素材按 adType 校验：image→imageUrl / text→textContent / code→htmlCode） */
    @ResponseBody
    @PostMapping("/ad/save")
    public DataVo adSave(@RequestParam(value = "positionId", required = false) Long positionId,
                         @RequestParam(value = "name", required = false) String name,
                         @RequestParam(value = "adType", defaultValue = "image") String adType,
                         @RequestParam(value = "imageUrl", required = false) String imageUrl,
                         @RequestParam(value = "url", required = false) String url,
                         @RequestParam(value = "textContent", required = false) String textContent,
                         @RequestParam(value = "htmlCode", required = false) String htmlCode,
                         @RequestParam(value = "weight", defaultValue = "1") int weight,
                         @RequestParam(value = "startTime", required = false)
                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                         @RequestParam(value = "endTime", required = false)
                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
                         @RequestParam(value = "status", defaultValue = "1") int status,
                         @RequestParam(value = "remark", required = false) String remark,
                         @RequestParam(value = "sort", defaultValue = "0") int sort) {
        requirePermission("/api/system/ad/ad/save");
        Ad form = new Ad();
        form.setPositionId(positionId);
        form.setName(name);
        form.setAdType(adType);
        form.setImageUrl(StringUtils.trimToNull(imageUrl));
        form.setUrl(StringUtils.trimToNull(url));
        form.setTextContent(textContent);
        form.setHtmlCode(htmlCode);
        form.setWeight(weight);
        form.setStartTime(startTime);
        form.setEndTime(endTime);
        form.setStatus(status);
        form.setRemark(remark);
        form.setSort(sort);
        return adService.addAd(form);
    }

    /** 编辑广告（id 必传；所属广告位不可改） */
    @ResponseBody
    @PostMapping("/ad/update")
    public DataVo adUpdate(@RequestParam(value = "id", required = false) Long id,
                           @RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "adType", defaultValue = "image") String adType,
                           @RequestParam(value = "imageUrl", required = false) String imageUrl,
                           @RequestParam(value = "url", required = false) String url,
                           @RequestParam(value = "textContent", required = false) String textContent,
                           @RequestParam(value = "htmlCode", required = false) String htmlCode,
                           @RequestParam(value = "weight", defaultValue = "1") int weight,
                           @RequestParam(value = "startTime", required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                           @RequestParam(value = "endTime", required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
                           @RequestParam(value = "status", defaultValue = "1") int status,
                           @RequestParam(value = "remark", required = false) String remark,
                           @RequestParam(value = "sort", defaultValue = "0") int sort) {
        requirePermission("/api/system/ad/ad/update");
        Ad form = new Ad();
        form.setId(id);
        form.setName(name);
        form.setAdType(adType);
        form.setImageUrl(StringUtils.trimToNull(imageUrl));
        form.setUrl(StringUtils.trimToNull(url));
        form.setTextContent(textContent);
        form.setHtmlCode(htmlCode);
        form.setWeight(weight);
        form.setStartTime(startTime);
        form.setEndTime(endTime);
        form.setStatus(status);
        form.setRemark(remark);
        form.setSort(sort);
        return adService.updateAd(form);
    }

    /** 按日统计（adId/positionId 可选过滤，days 默认 30，上限 365），行：statDate/views/clicks */
    @ResponseBody
    @GetMapping("/stat/list")
    public DataVo statList(@RequestParam(value = "adId", required = false) Long adId,
                           @RequestParam(value = "positionId", required = false) Long positionId,
                           @RequestParam(value = "days", defaultValue = "30") int days) {
        requirePermission("/api/system/ad/stat/list");
        return DataVo.success("操作成功", adService.listDailyStat(adId, positionId, days));
    }

    /** 删除广告 */
    @ResponseBody
    @PostMapping("/ad/delete")
    public DataVo adDelete(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/ad/ad/delete");
        if (id == null || id <= 0) {
            return DataVo.failure("广告 id 不能为空");
        }
        return adService.deleteAd(id);
    }
}
