package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.announcement.model.Announcement;
import com.flycms.module.announcement.service.AnnouncementService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

import java.util.Map;

/**
 * 公告管理 REST（规划：参照 DedeCMS mynews / 帝国公告）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiAnnouncementController extends ApiBaseController {

    @Autowired
    private AnnouncementService announcementService;

    @ResponseBody
    @GetMapping("/system/announcement/list")
    public DataVo list(@RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/api/system/announcement/list");
        PageVo<Announcement> pageVo = announcementService.getAnnouncementPage(pageNum, 20);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/announcement/{id}")
    public DataVo detail(@PathVariable Long id) {
        requirePermission("/api/system/announcement/list");
        Announcement announcement = announcementService.findAnnouncementById(id);
        if (announcement == null) {
            return DataVo.failure("公告不存在");
        }
        return DataVo.success("操作成功", announcement);
    }

    @ResponseBody
    @PostMapping("/system/announcement/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/announcement/save");
        Announcement announcement;
        try {
            announcement = build(params);
        } catch (ParseException e) {
            return DataVo.failure("时间格式不正确（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）");
        }
        return announcementService.addAnnouncement(announcement);
    }

    @ResponseBody
    @PostMapping("/system/announcement/update")
    public DataVo update(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/announcement/save");
        Long id = parseLong(params.get("id"));
        if (id == null) {
            return DataVo.failure("参数传递错误");
        }
        Announcement announcement;
        try {
            announcement = build(params);
        } catch (ParseException e) {
            return DataVo.failure("时间格式不正确（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）");
        }
        announcement.setId(id);
        return announcementService.updateAnnouncement(announcement);
    }

    @ResponseBody
    @PostMapping("/system/announcement/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/announcement/delete");
        return announcementService.deleteAnnouncement(id);
    }

    private Announcement build(Map<String, String> params) throws ParseException {
        Announcement a = new Announcement();
        a.setTitle(params.get("title"));
        a.setContent(params.get("content"));
        a.setLinkUrl(StringUtils.trimToNull(params.get("linkUrl")));
        a.setStartTime(AnnouncementService.parseTimeParam(params.get("startTime")));
        a.setEndTime(AnnouncementService.parseTimeParam(params.get("endTime")));
        a.setStatus(parseInt(params.get("status"), 1));
        a.setSort(parseInt(params.get("sort"), 0));
        return a;
    }

    private Long parseLong(String v) {
        try {
            return v == null ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
