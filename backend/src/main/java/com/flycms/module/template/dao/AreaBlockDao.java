package com.flycms.module.template.dao;

import com.flycms.module.template.model.AreaBlock;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 区域区块编排 DAO（规划 §12.1 / P10）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AreaBlockDao {

    /** 某主题下全部区块（含停用，后台管理用） */
    List<AreaBlock> listByTheme(@Param("themeCode") String themeCode);

    /** 某主题某区域的<b>启用</b>区块，按 sort 升序（前台渲染用） */
    List<AreaBlock> listEnabled(@Param("themeCode") String themeCode, @Param("areaName") String areaName);

    AreaBlock selectById(@Param("id") Long id);

    int insert(AreaBlock block);

    int update(AreaBlock block);

    int deleteById(@Param("id") Long id);

    int updateSort(@Param("id") Long id, @Param("sort") Integer sort);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 该区域当前最大 sort（新增时排到末尾） */
    Integer maxSort(@Param("themeCode") String themeCode, @Param("areaName") String areaName);

    /** 区域名清单（该主题下已编排过区块的区域），供后台布局管理页左栏 */
    List<String> listAreaNames(@Param("themeCode") String themeCode);
}
