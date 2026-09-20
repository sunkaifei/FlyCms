package com.flycms.module.model.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 自定义模型动态数据表 DAO（fly_cmodel_{modelId}）。
 *
 * ${} 仅用于已过 SqlSafeUtil 白名单的标识符（modelId 数字、列名、whereSql/orderBySql——
 * 二者必须由 Service 依元数据生成，其中参数占位一律 #{params 内的 key}）；
 * 业务值永远 #{}。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface ModelDataDao {

    // /////////////////// DDL（固定列模板见手册 §4.3） ///////////////////

    public void createTable(@Param("modelId") String modelId);

    public void addColumn(@Param("modelId") String modelId, @Param("columnName") String columnName,
                          @Param("columnType") String columnType, @Param("comment") String comment);

    public void modifyColumn(@Param("modelId") String modelId, @Param("columnName") String columnName,
                             @Param("columnType") String columnType);

    public void dropColumn(@Param("modelId") String modelId, @Param("columnName") String columnName);

    public void dropTable(@Param("modelId") String modelId);

    public boolean tableExists(@Param("modelId") String modelId);

    // /////////////////// DML ///////////////////

    public void insertData(@Param("modelId") String modelId, @Param("columns") List<String> columns,
                           @Param("values") Map<String, Object> values);

    public void updateData(@Param("modelId") String modelId, @Param("id") Long id,
                           @Param("columns") List<String> columns, @Param("values") Map<String, Object> values);

    public Map<String, Object> findDataById(@Param("modelId") String modelId, @Param("id") Long id);

    public Map<String, Object> findByShortUrl(@Param("modelId") String modelId, @Param("shortUrl") String shortUrl);

    public boolean existsShortUrl(@Param("modelId") String modelId, @Param("shortUrl") String shortUrl);

    public List<Map<String, Object>> selectPage(@Param("modelId") String modelId, @Param("whereSql") String whereSql,
                                                @Param("orderBySql") String orderBySql,
                                                @Param("params") Map<String, Object> params);

    public int countPage(@Param("modelId") String modelId, @Param("whereSql") String whereSql,
                         @Param("params") Map<String, Object> params);

    public int updateStatus(@Param("modelId") String modelId, @Param("ids") List<Long> ids, @Param("status") int status);

    public int deleteData(@Param("modelId") String modelId, @Param("ids") List<Long> ids);

    // /////////////////// 附件（fly_images，AttachmentPicker 数据源） ///////////////////

    public List<Map<String, Object>> attachmentList(@Param("offset") int offset, @Param("rows") int rows);

    public int attachmentCount();

    public boolean existsImages(@Param("ids") List<Long> ids);

    public java.util.List<Map<String, Object>> findImageUrls(@Param("ids") List<Long> ids);

    public void incrImagesRefCount(@Param("ids") List<Long> ids);

    public void decrImagesRefCount(@Param("ids") List<Long> ids);
}
