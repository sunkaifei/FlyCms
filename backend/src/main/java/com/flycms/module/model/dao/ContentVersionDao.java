package com.flycms.module.model.dao;

import com.flycms.module.model.model.ContentVersion;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 内容版本快照 DAO（M/G12）。targetModel 走 SqlSafeUtil.safeModelCode 白名单后由调用方传入。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ContentVersionDao {

    int addVersion(ContentVersion version);

    /** 某内容当前最大版本号（无版本返回 0） */
    int maxVersion(@Param("targetModel") String targetModel, @Param("targetId") Long targetId);

    ContentVersion findVersion(@Param("targetModel") String targetModel,
                               @Param("targetId") Long targetId,
                               @Param("version") int version);

    List<ContentVersion> selectVersions(@Param("targetModel") String targetModel,
                                        @Param("targetId") Long targetId,
                                        @Param("offset") int offset,
                                        @Param("rows") int rows);

    int countVersions(@Param("targetModel") String targetModel, @Param("targetId") Long targetId);

    /** 模型删除时清理其全部版本快照 */
    int deleteByTargetModel(@Param("targetModel") String targetModel);

    /**
     * 恢复回写：setSql 由 Service 拼接（形如 {@code `col` = #{p0}, `col2` = #{p1}}），
     * 列名过标识符正则、参数键 p0..pn 与 setSql 一一对应，值恒走 #{} 预编译。
     * （不用 foreach+OGNL 动态键：params[c] 无引号索引解析不到会静默绑 null，2026-09-29 探针暴露。）
     */
    int restoreColumns(@Param("suffix") String suffix,
                       @Param("setSql") String setSql,
                       @Param("id") Long id,
                       @Param("params") Map<String, Object> params);
}
