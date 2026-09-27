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

    public List<Map<String, Object>> selectPage(@Param("suffix") String suffix, @Param("whereSql") String whereSql,
                                                @Param("orderBySql") String orderBySql,
                                                @Param("params") Map<String, Object> params);

    public int countPage(@Param("suffix") String suffix, @Param("whereSql") String whereSql,
                         @Param("params") Map<String, Object> params);

    public int updateStatus(@Param("suffix") String suffix, @Param("ids") List<Long> ids, @Param("status") int status);

    public int deleteData(@Param("suffix") String suffix, @Param("ids") List<Long> ids);

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
