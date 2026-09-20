package com.flycms.module.model.service;

import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.ModelDataDao;
import com.flycms.module.model.enums.FieldTypeEnum;
import com.flycms.module.model.model.ModelField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 动态 DDL 服务：模型数据表（fly_cmodel_{modelId}）的建表与列维护。
 * 项目唯一的动态建表通道。安全边界：进入 DDL 的只有雪花数字 ID（safeNumber）、
 * 白名单字段名（safeColumnName + 反引号）、FieldTypeEnum 推导的列类型。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelTableService {

    @Autowired
    private ModelDataDao modelDataDao;

    /**
     * 新建模型后调用：建固定列表 + 按字段定义生成自定义列。调用方保证在元数据保存成功之后执行。
     */
    public void createModelTable(Long modelId, List<ModelField> fields) {
        String mid = SqlSafeUtil.safeNumber(modelId);
        modelDataDao.createTable(mid);
        if (fields != null) {
            for (ModelField field : fields) {
                addColumn(modelId, field);
            }
        }
    }

    /**
     * 新增自定义字段 → ALTER ADD COLUMN（editor 不建列）
     */
    public void addColumn(Long modelId, ModelField field) {
        FieldTypeEnum type = FieldTypeEnum.of(field.getFieldType());
        String colType = type.resolveColumnType(field.getMaxlength());
        if (colType == null) {
            return;
        }
        modelDataDao.addColumn(SqlSafeUtil.safeNumber(modelId),
                SqlSafeUtil.safeColumnName(field.getFieldName()),
                colType, field.getFieldLabel() == null ? "" : field.getFieldLabel().replace("'", "''"));
    }

    /**
     * 修改字段（仅同兼容组：varchar 加长）。跨组变更由调用方拒绝，这里只做 MODIFY。
     */
    public void modifyColumn(Long modelId, String fieldName, String newColumnType) {
        modelDataDao.modifyColumn(SqlSafeUtil.safeNumber(modelId),
                SqlSafeUtil.safeColumnName(fieldName), newColumnType);
    }

    /**
     * 删除字段 → DROP COLUMN
     */
    public void dropColumn(Long modelId, String fieldName) {
        modelDataDao.dropColumn(SqlSafeUtil.safeNumber(modelId), SqlSafeUtil.safeColumnName(fieldName));
    }

    /**
     * 删除模型 → DROP TABLE IF EXISTS（幂等）
     */
    public void dropTable(Long modelId) {
        modelDataDao.dropTable(SqlSafeUtil.safeNumber(modelId));
    }

    /**
     * 表是否存在（供懒补偿与删除前校验）
     */
    public boolean tableExists(Long modelId) {
        return modelDataDao.tableExists(SqlSafeUtil.safeNumber(modelId));
    }

    /**
     * 表存在则跳过，不存在则按当前字段定义重建（懒补偿，幂等）
     */
    public void ensureTable(Long modelId, List<ModelField> fields) {
        if (!tableExists(modelId)) {
            createModelTable(modelId, fields);
        }
    }
}
