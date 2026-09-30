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

    /** P2 反向引用：所有指向目标模型 code 的单值 RELATE 字段（跨模型） */
    public List<ModelField> findRelateFieldsByTargetModel(@Param("code") String code);

    /** P1：删除 GROUP/REPEATER 字段时级联删除其子字段元数据 */
    public int deleteFieldsByParentId(@Param("parentId") Long parentId);

    public int updateFieldSort(@Param("id") Long id, @Param("sort") int sort);

    /** 绑定指定字典类型的字段数（字典删除前防悬空引用检查） */
    public int countFieldsByDictType(@Param("dictType") String dictType);
}
