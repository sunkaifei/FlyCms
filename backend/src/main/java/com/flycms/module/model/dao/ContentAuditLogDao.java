package com.flycms.module.model.dao;

import com.flycms.module.model.model.ContentAuditLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 内容审核日志 DAO（2026-10-23）。targetModel 由调用方过 {@code safeModelCode} 白名单。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Repository
public interface ContentAuditLogDao {

    int addLog(ContentAuditLog log);

    List<ContentAuditLog> selectByTarget(@Param("targetModel") String targetModel,
                                        @Param("targetId") Long targetId,
                                        @Param("offset") int offset,
                                        @Param("rows") int rows);

    int countByTarget(@Param("targetModel") String targetModel,
                      @Param("targetId") Long targetId);

    /** 模型删除时清理其全部审核日志 */
    int deleteByTargetModel(@Param("targetModel") String targetModel);
}
