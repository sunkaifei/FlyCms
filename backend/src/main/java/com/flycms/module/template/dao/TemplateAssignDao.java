package com.flycms.module.template.dao;

import com.flycms.module.template.model.TemplateAssign;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模板指派 DAO（规划 §5.3）。按 target_type/target_id/page_type 唯一确定一条指派，
 * 解析时优先级高于文件名层级。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface TemplateAssignDao {

    /** 按 (targetType, targetId, pageType) 唯一 upsert */
    void upsert(@Param("assign") TemplateAssign assign);

    /** 查询某目标在某页面类型上指派到的模板相对路径，无则 null */
    TemplateAssign findByTarget(@Param("targetType") String targetType,
                                @Param("targetId") String targetId,
                                @Param("pageType") String pageType);

    /** 删除某目标的某页面类型指派 */
    int deleteByTarget(@Param("targetType") String targetType,
                       @Param("targetId") String targetId,
                       @Param("pageType") String pageType);

    /** 列举某目标类型的全部指派（后台选择器/审计用） */
    List<TemplateAssign> listByTargetType(@Param("targetType") String targetType);
}
