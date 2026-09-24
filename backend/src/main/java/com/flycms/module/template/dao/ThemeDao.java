package com.flycms.module.template.dao;

import com.flycms.module.template.model.Theme;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 主题注册表 DAO（规划 §12.1）。表 {@code fly_theme} 用作扫描结果的<b>登记/展示</b>，
 * 解析仍以磁盘 {@code theme.json} 为事实源（filesystem is source of truth），本 DAO 不阻塞渲染。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ThemeDao {

    /** 按 code 唯一 upsert（扫描皮肤目录后登记） */
    void upsert(@Param("theme") Theme theme);

    /** 全量列举（后台主题市场页数据源；排序由调用方处理） */
    List<Theme> listAll();

    /** 查询单个主题 */
    Theme selectByCode(@Param("code") String code);

    /** 标记当前使用主题（清掉其它记录的 is_current，置该 code 为 1） */
    void updateCurrent(@Param("code") String code);

    /** 清除全部 is_current 标记 */
    void clearCurrent();
}
