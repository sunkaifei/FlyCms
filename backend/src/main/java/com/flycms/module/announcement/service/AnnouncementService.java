package com.flycms.module.announcement.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.announcement.dao.AnnouncementDao;
import com.flycms.module.announcement.model.Announcement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 网站公告服务
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementDao announcementDao;

    public PageVo<Announcement> getAnnouncementPage(int pageNum, int rows) {
        PageVo<Announcement> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(announcementDao.getAnnouncementPage(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(announcementDao.getAnnouncementCount());
        return pageVo;
    }

    /** 前台可见公告（启用 + 时间窗内） */
    public List<Announcement> getVisibleAnnouncements(int rows) {
        return announcementDao.getVisibleAnnouncements(rows);
    }

    public Announcement findAnnouncementById(Long id) {
        return announcementDao.findAnnouncementById(id);
    }

    public DataVo addAnnouncement(Announcement form) {
        if (StringUtils.isBlank(form.getTitle())) {
            return DataVo.failure("公告标题不能为空");
        }
        form.setId(new SnowFlake(2, 3).nextId());
        form.setStatus(form.getStatus() == 0 ? 1 : form.getStatus());
        form.setCreateTime(new Date());
        announcementDao.insertAnnouncement(form);
        return DataVo.success("公告已添加");
    }

    public DataVo updateAnnouncement(Announcement form) {
        if (form.getId() == null || announcementDao.findAnnouncementById(form.getId()) == null) {
            return DataVo.failure("公告不存在");
        }
        if (StringUtils.isBlank(form.getTitle())) {
            return DataVo.failure("公告标题不能为空");
        }
        announcementDao.updateAnnouncement(form);
        return DataVo.success("公告已更新");
    }

    public DataVo deleteAnnouncement(Long id) {
        announcementDao.deleteAnnouncementById(id);
        return DataVo.success("公告已删除");
    }

    /** 从表单字符串解析时间（供 controller 用 @RequestParam 场景） */
    public static Date parseTimeParam(String value) throws ParseException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        value = value.trim().replace('T', ' ');
        SimpleDateFormat fmt = value.length() > 10
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                : new SimpleDateFormat("yyyy-MM-dd");
        return fmt.parse(value);
    }
}
