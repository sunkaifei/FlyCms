package com.flycms.module.announcement.dao;

import com.flycms.module.announcement.model.Announcement;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 网站公告 DAO
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AnnouncementDao {

    public void insertAnnouncement(Announcement announcement);

    public void updateAnnouncement(Announcement announcement);

    public void deleteAnnouncementById(@Param("id") Long id);

    public Announcement findAnnouncementById(@Param("id") Long id);

    public List<Announcement> getAnnouncementPage(@Param("offset") int offset, @Param("rows") int rows);

    public int getAnnouncementCount();

    /** 前台可见：启用 + 时间窗内 */
    public List<Announcement> getVisibleAnnouncements(@Param("rows") int rows);
}
