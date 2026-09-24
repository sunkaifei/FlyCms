package com.flycms.module.template.dao;

import com.flycms.module.template.model.TemplateFile;
import com.flycms.module.template.model.TemplateVersion;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模板登记与版本快照 DAO（规划阶段 D）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface TemplateDao {

    // /////////////////// 模板登记 ///////////////////

    void insertTemplate(TemplateFile template);

    void updateTemplateTime(TemplateFile template);

    void deleteTemplate(@Param("skin") String skin, @Param("filePath") String filePath);

    void deleteTemplatesBySkin(@Param("skin") String skin);

    TemplateFile findTemplate(@Param("skin") String skin, @Param("filePath") String filePath);

    List<TemplateFile> findTemplatesBySkin(@Param("skin") String skin);

    // /////////////////// 版本快照 ///////////////////

    void insertVersion(TemplateVersion version);

    TemplateVersion findVersion(@Param("templateId") Long templateId, @Param("version") int version);

    List<TemplateVersion> findVersions(@Param("templateId") Long templateId, @Param("rows") int rows);

    int maxVersion(@Param("templateId") Long templateId);

    int countVersions(@Param("templateId") Long templateId);

    void deleteVersions(@Param("templateId") Long templateId);
}
