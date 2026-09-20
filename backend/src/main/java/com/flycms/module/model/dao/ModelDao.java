package com.flycms.module.model.dao;

import com.flycms.module.model.model.Model;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 内容模型定义 DAO
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ModelDao {

    public int addModel(Model model);

    public int updateModel(Model model);

    public int deleteModelById(@Param("id") Long id);

    public Model findModelById(@Param("id") Long id);

    public Model findModelByCode(@Param("code") String code);

    public boolean checkModelCode(@Param("code") String code);

    public List<Model> getModelList(@Param("offset") int offset, @Param("rows") int rows);

    public int getModelCount();

    public List<Model> getAllModelList(@Param("status") Integer status);
}
