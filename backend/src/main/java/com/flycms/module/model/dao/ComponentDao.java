package com.flycms.module.model.dao;

import com.flycms.module.model.model.Component;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字段组库 DAO（G15）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ComponentDao {

    List<Component> findAll();

    Component findById(@Param("id") Long id);

    Component findByCode(@Param("code") String code);

    int add(Component component);

    int update(Component component);

    int delete(@Param("id") Long id);
}
