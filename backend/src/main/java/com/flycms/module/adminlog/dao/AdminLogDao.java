package com.flycms.module.adminlog.dao;

import com.flycms.module.adminlog.model.AdminLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 管理操作审计 DAO
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AdminLogDao {

    public void insertLog(AdminLog log);

    public List<AdminLog> getLogPage(@Param("adminName") String adminName, @Param("path") String path,
                                     @Param("startTime") String startTime, @Param("endTime") String endTime,
                                     @Param("offset") int offset, @Param("rows") int rows);

    public int getLogCount(@Param("adminName") String adminName, @Param("path") String path,
                           @Param("startTime") String startTime, @Param("endTime") String endTime);
}
