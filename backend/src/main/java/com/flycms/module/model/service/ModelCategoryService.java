package com.flycms.module.model.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.model.dao.ModelCategoryDao;
import com.flycms.module.model.model.ModelCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型通用分类服务（fly_model_category，model_id 维度树形）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelCategoryService {

    @Autowired
    private ModelCategoryDao modelCategoryDao;

    public List<ModelCategory> findCategoriesByModelId(Long modelId, Integer status) {
        return modelCategoryDao.findCategoriesByModelId(modelId, status);
    }

    public ModelCategory findCategoryById(Long id) {
        return modelCategoryDao.findCategoryById(id);
    }

    public DataVo addCategory(ModelCategory category) {
        if (category.getFatherId() == null) {
            category.setFatherId(0L);
        }
        if (category.getFatherId() != 0 && modelCategoryDao.findCategoryById(category.getFatherId()) == null) {
            return DataVo.failure("上级分类不存在");
        }
        category.setStatus(1);
        modelCategoryDao.addCategory(category);
        return DataVo.success("分类已添加");
    }

    public DataVo updateCategory(ModelCategory form) {
        if (modelCategoryDao.findCategoryById(form.getId()) == null) {
            return DataVo.failure("分类不存在");
        }
        modelCategoryDao.updateCategory(form);
        return DataVo.success("分类已更新");
    }

    public DataVo deleteCategory(Long id) {
        if (modelCategoryDao.hasChildren(id)) {
            return DataVo.failure("请先删除下级分类");
        }
        modelCategoryDao.deleteCategoryById(id);
        return DataVo.success("分类已删除");
    }
}
