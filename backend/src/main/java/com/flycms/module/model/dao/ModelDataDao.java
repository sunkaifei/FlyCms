package com.flycms.module.model.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 自定义模型动态数据表 DAO（fly_cmodel_{suffix}，suffix = 模型 code）。
 *
 * ${} 仅用于已过 SqlSafeUtil 白名单的标识符（suffix 走 safeTableSuffix/safeTableSuffixForExisting、
 * 列名走 safeColumnName、
 * whereSql/orderBySql 由 Service 依元数据生成且参数占位一律 #{params 内的 key}）；
 * 业务值永远 #{}。
 *
 * @author sun-kaifei
 * @version 1.1
 */
@Repository
public interface ModelDataDao {

    // /////////////////// DDL ///////////////////

    public void createTable(@Param("suffix") String suffix);

    public void addColumn(@Param("suffix") String suffix, @Param("columnName") String columnName,
                          @Param("columnType") String columnType, @Param("comment") String comment);

    public void modifyColumn(@Param("suffix") String suffix, @Param("columnName") String columnName,
                             @Param("columnType") String columnType);

    public void dropColumn(@Param("suffix") String suffix, @Param("columnName") String columnName);

    public void dropTable(@Param("suffix") String suffix);

    public boolean tableExists(@Param("suffix") String suffix);

    /** 表名冲突预检（新建模型前调用，D7） */
    public boolean tableExistsBySuffix(@Param("suffix") String suffix);

    // /////////////////// DML ///////////////////

    public void insertData(@Param("suffix") String suffix, @Param("columns") List<String> columns,
                           @Param("values") Map<String, Object> values);

    public void updateData(@Param("suffix") String suffix, @Param("id") Long id,
                           @Param("columns") List<String> columns, @Param("values") Map<String, Object> values);

    public Map<String, Object> findDataById(@Param("suffix") String suffix, @Param("id") Long id);

    public Map<String, Object> findByShortUrl(@Param("suffix") String suffix, @Param("shortUrl") String shortUrl);

    public boolean existsShortUrl(@Param("suffix") String suffix, @Param("shortUrl") String shortUrl);

    /**
     * 动态列表查询（列白名单投影，G11）。
     *
     * @param columns 投影列清单（不含反引号；每个元素由 Service 依「固定列 + 启用模型字段」生成，
     *                并已过 {@code SqlSafeUtil.safeColumnName}），XML 侧统一加反引号输出。
     *                传空则回退为全列，保证向后兼容。
     */
    public List<Map<String, Object>> selectPage(@Param("suffix") String suffix, @Param("whereSql") String whereSql,
                                                @Param("orderBySql") String orderBySql,
                                                @Param("columns") List<String> columns,
                                                @Param("params") Map<String, Object> params);

    public int countPage(@Param("suffix") String suffix, @Param("whereSql") String whereSql,
                         @Param("params") Map<String, Object> params);

    public int updateStatus(@Param("suffix") String suffix, @Param("ids") List<Long> ids, @Param("status") int status);

    public int deleteData(@Param("suffix") String suffix, @Param("ids") List<Long> ids);

    // /////////////////// 标签聚合（前台 /tag/{tag}/，§5.1 标签页） ///////////////////

    /**
     * 按关键词在单个模型表内做"标签式"检索：命中 {@code title} / {@code keywords}，
     * 表内存在 {@code tags} 列时（见 {@code hasTags}）一并命中。
     *
     * <p>与 {@link #selectPage} 的区别：这里只做前台展示所需的固定列裁剪 + 按发布时间倒序 +
     * 每表 LIMIT，供跨模型合并分页使用（前端标签页的语义就是"这个关键词下有哪些内容"）。
     *
     * @param suffix   模型 code（已过白名单）
     * @param keyword  关键词（业务值，恒走 #{}）
     * @param hasTags  该表是否存在 tags 列（由调用方用 information_schema 探测后缓存）
     * @param limit    本表最多取多少行
     */
    public List<Map<String, Object>> searchByKeyword(@Param("suffix") String suffix,
                                                     @Param("keyword") String keyword,
                                                     @Param("hasTags") boolean hasTags,
                                                     @Param("limit") int limit);

    /** searchByKeyword 的总数（用于合并分页的准确总数） */
    public int countByKeyword(@Param("suffix") String suffix,
                              @Param("keyword") String keyword,
                              @Param("hasTags") boolean hasTags);

    /** 探测某表是否含指定列（information_schema，结果由调用方缓存） */
    public boolean columnExists(@Param("suffix") String suffix, @Param("columnName") String columnName);

    /** 唯一约束校验：列名来自字段元数据白名单（非用户输入），排除 excludeId（更新场景） */
    public int countDuplicate(@Param("suffix") String suffix, @Param("columnName") String columnName,
                              @Param("value") Object value, @Param("excludeId") Long excludeId);

    /**
     * E1 关联展开：按 id 批量取目标模型内容的轻量列（{@code id / short_url / title / status}）。
     *
     * <p>只取展示必需列，避免关联展开把目标表的 longtext 正文拉回内存；
     * 调用方（{@code ModelDataService.expandReferences}）以「一次批量查询」满足
     * 「模板零二次查询」原则（模型手册 §7.3）。
     */
    public List<Map<String, Object>> findRowsByIds(@Param("suffix") String suffix, @Param("ids") List<Long> ids);

    // /////////////////// 附件（fly_images，AttachmentPicker 数据源） ///////////////////

    public List<Map<String, Object>> attachmentList(@Param("offset") int offset, @Param("rows") int rows);

    public int attachmentCount();

    /**
     * 统计 fly_images 中真实存在的附件 id 数（配合 ids.size() 在 Java 侧判断"全部存在"）。
     * 不要在 #{} 占位符里写 ids.size() 这类方法调用——MyBatis 只支持属性导航，会抛
     * ReflectionException（2026-09-27 修复：原写法被 Service 的 catch 吞成"处理失败"）。
     */
    public int countImages(@Param("ids") List<Long> ids);

    public java.util.List<Map<String, Object>> findImageUrls(@Param("ids") List<Long> ids);

    public void incrImagesRefCount(@Param("ids") List<Long> ids);

    public void decrImagesRefCount(@Param("ids") List<Long> ids);
}
