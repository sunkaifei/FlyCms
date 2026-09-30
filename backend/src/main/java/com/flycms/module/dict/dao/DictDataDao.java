package com.flycms.module.dict.dao;

import com.flycms.module.dict.model.DictData;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据字典数据 DAO。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface DictDataDao {

    /** 分页列表（按字典类型，label 模糊可选） */
    List<DictData> findDictDatas(@Param("dictType") String dictType, @Param("keyword") String keyword,
                                 @Param("offset") int offset, @Param("rows") int rows);

    int countDictDatas(@Param("dictType") String dictType, @Param("keyword") String keyword);

    DictData findDictDataById(@Param("id") Long id);

    /** 同类型下 dict_value 是否已存在（排除指定 id） */
    int checkDictValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue,
                       @Param("excludeId") Long excludeId);

    int addDictData(DictData dictData);

    int updateDictData(DictData dictData);

    int deleteDictDataById(@Param("id") Long id);

    /** 删除某字典类型的全部数据（删类型时级联） */
    int deleteDictDataByType(@Param("dictType") String dictType);

    /** 某类型的启用数据（表单候选项，按 sort, id 排序） */
    List<DictData> findEnabledByType(@Param("dictType") String dictType);
}
