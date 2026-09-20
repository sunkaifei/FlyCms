package com.flycms.module.model.dao;

import com.flycms.module.model.model.ModelCategory;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型通用分类 DAO（fly_model_category）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ModelCategoryDao {

    public void addCategory(ModelCategory category);

    public int updateCategory(ModelCategory category);

    public int deleteCategoryById(@Param("id") Long id);

    public ModelCategory findCategoryById(@Param("id") Long id);

    public List<ModelCategory> findCategoriesByModelId(@Param("modelId") Long modelId, @Param("status") Integer status);

    public boolean hasChildren(@Param("id") Long id);
}
