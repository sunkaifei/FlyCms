package com.flycms.module.model.dao;

import com.flycms.module.model.model.ModelField;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型字段定义 DAO
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ModelFieldDao {

    public int addField(ModelField field);

    public int updateField(ModelField field);

    public int deleteFieldById(@Param("id") Long id);

    public int deleteFieldsByModelId(@Param("modelId") Long modelId);

    public ModelField findFieldById(@Param("id") Long id);

    public boolean checkFieldName(@Param("modelId") Long modelId, @Param("fieldName") String fieldName);

    public List<ModelField> findFieldsByModelId(@Param("modelId") Long modelId, @Param("status") Integer status);

    public int updateFieldSort(@Param("id") Long id, @Param("sort") int sort);
}
