package com.flycms.module.dict.dao;

import com.flycms.module.dict.model.DictType;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据字典类型 DAO。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface DictTypeDao {

    /** 分页列表（keyword 模糊匹配名称/类型键） */
    List<DictType> findDictTypes(@Param("offset") int offset, @Param("rows") int rows,
                                 @Param("keyword") String keyword);

    int countDictTypes(@Param("keyword") String keyword);

    DictType findDictTypeById(@Param("id") Long id);

    DictType findDictTypeByKey(@Param("dictType") String dictType);

    /** dict_type 键是否已存在（排除指定 id，编辑场景用） */
    int checkDictTypeKey(@Param("dictType") String dictType, @Param("excludeId") Long excludeId);

    int addDictType(DictType dictType);

    int updateDictType(DictType dictType);

    int deleteDictTypeById(@Param("id") Long id);

    /** 全部启用类型（字段设置「绑定字典」下拉用） */
    List<DictType> findEnabledDictTypes();
}
