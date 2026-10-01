package com.flycms.module.model.dao;

import com.flycms.module.model.model.ModelGroup;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型分组 DAO（fly_model_group，① 组织层）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ModelGroupDao {

    void insert(ModelGroup group);

    int update(ModelGroup group);

    int deleteById(@Param("id") Long id);

    ModelGroup findById(@Param("id") Long id);

    ModelGroup findByCode(@Param("code") String code);

    /** 全部分组（sort asc, id asc）；status 非 null 时过滤 */
    List<ModelGroup> findAll(@Param("status") Integer status);

    /** 分组内模型数（删分组前置校验：组内模型先移出） */
    int countModels(@Param("groupId") Long groupId);
}
