package com.flycms.module.redirect.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * SEO 重定向规则 DAO（fly_redirect，T-b）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface RedirectDao {

    /** 全部启用规则行（source/target 列），Service 层组装路径映射 */
    List<Map<String, Object>> allEnabledRows();

    String findIdBySource(@Param("sourcePath") String sourcePath);

    void insert(@Param("id") Long id, @Param("sourcePath") String sourcePath,
                @Param("targetUrl") String targetUrl, @Param("status") int status);

    void update(@Param("id") String id, @Param("targetUrl") String targetUrl, @Param("status") int status);

    void delete(@Param("id") String id);
}
