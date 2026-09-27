package com.flycms.module.model.service;

import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.ModelDataDao;
import com.flycms.module.model.enums.FieldTypeEnum;
import com.flycms.module.model.model.ModelField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 动态 DDL 服务：模型数据表（fly_cmodel_{code}）的建表与列维护。
 * 项目唯一的动态建表通道。
 *
 * <p><b>安全边界（D7/D10）</b>：进入 DDL 的只有模型 code（safeTableSuffixForExisting
 * 标识符白名单 + SQL 保留字黑名单）、白名单字段名（safeColumnName + 反引号）、
 * FieldTypeEnum 推导的列类型。自 v1.1 起表名后缀由 {@link SqlSafeUtil} 校验，
 * 不再使用纯数字的 safeNumber —— 表名从雪花 ID 改为可读 code。
 *
 * <p><b>黑名单分层（2026-09-27）</b>：本服务全部走存量豁免校验
 * {@link SqlSafeUtil#safeTableSuffixForExisting(String)}（只拦 SQL 保留字）——
 * 本服务的操作对象都是已存在的物理表（含种子模型 images → fly_cmodel_images），
 * 项目表名黑名单（防混淆）只在新建模型业务边界 {@code ModelService.addModel} 生效。
 *
 * @author sun-kaifei
 * @version 1.2
 */
@Service
public class ModelTableService {

    @Autowired
    private ModelDataDao modelDataDao;

    /**
     * 新建模型后调用：建固定列表 + 按字段定义生成自定义列。调用方保证在元数据保存成功之后执行。
     *
     * @param code 模型 code（即物理表名后缀，如 articles → fly_cmodel_articles）
     */
    public void createModelTable(String code, List<ModelField> fields) {
        String suffix = SqlSafeUtil.safeTableSuffixForExisting(code);
        modelDataDao.createTable(suffix);
        if (fields != null) {
            for (ModelField field : fields) {
                addColumn(code, field);
            }
        }
    }

    /**
     * 新增自定义字段 → ALTER ADD COLUMN（editor 不建列）
     */
    public void addColumn(String code, ModelField field) {
        FieldTypeEnum type = FieldTypeEnum.of(field.getFieldType());
        String colType = type.resolveColumnType(field.getMaxlength());
        if (colType == null) {
            return;
        }
        modelDataDao.addColumn(SqlSafeUtil.safeTableSuffixForExisting(code),
                SqlSafeUtil.safeColumnName(field.getFieldName()),
                colType, field.getFieldLabel() == null ? "" : field.getFieldLabel().replace("'", "''"));
    }

    /**
     * 修改字段（仅同兼容组：varchar 加长）。跨组变更由调用方拒绝，这里只做 MODIFY。
     */
    public void modifyColumn(String code, String fieldName, String newColumnType) {
        modelDataDao.modifyColumn(SqlSafeUtil.safeTableSuffixForExisting(code),
                SqlSafeUtil.safeColumnName(fieldName), newColumnType);
    }

    /**
     * 删除字段 → DROP COLUMN
     */
    public void dropColumn(String code, String fieldName) {
        modelDataDao.dropColumn(SqlSafeUtil.safeTableSuffixForExisting(code), SqlSafeUtil.safeColumnName(fieldName));
    }

    /**
     * 删除模型 → DROP TABLE IF EXISTS（幂等）
     */
    public void dropTable(String code) {
        modelDataDao.dropTable(SqlSafeUtil.safeTableSuffixForExisting(code));
    }

    /**
     * 表是否存在（供懒补偿与删除前校验）
     */
    public boolean tableExists(String code) {
        return modelDataDao.tableExists(SqlSafeUtil.safeTableSuffixForExisting(code));
    }

    /**
     * 表存在则跳过，不存在则按当前字段定义重建（懒补偿，幂等）
     */
    public void ensureTable(String code, List<ModelField> fields) {
        if (!tableExists(code)) {
            createModelTable(code, fields);
        }
    }
}
