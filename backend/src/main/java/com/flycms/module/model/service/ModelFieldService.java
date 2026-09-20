package com.flycms.module.model.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.enums.FieldTypeEnum;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 模型字段元数据服务：字段增删改 + 类型兼容校验 + DDL 联动。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelFieldService {

    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private ModelTableService modelTableService;
    @Autowired
    private ModelService modelService;

    public List<ModelField> findFieldsByModelId(Long modelId, Integer status) {
        return modelFieldDao.findFieldsByModelId(modelId, status);
    }

    public ModelField findFieldById(Long id) {
        return modelFieldDao.findFieldById(id);
    }

    /**
     * 新增字段：元数据校验 → columnType 由 FieldTypeEnum 推导（前端不可传）→ 保存 → ALTER ADD COLUMN
     */
    public DataVo addField(ModelField field) {
        DataVo data = DataVo.failure("操作失败");
        Model model = modelService.findModelById(field.getModelId());
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        try {
            SqlSafeUtil.safeColumnName(field.getFieldName());
        } catch (IllegalArgumentException e) {
            return DataVo.failure(e.getMessage());
        }
        FieldTypeEnum type;
        try {
            type = FieldTypeEnum.of(field.getFieldType());
        } catch (IllegalArgumentException e) {
            return DataVo.failure(e.getMessage());
        }
        if (StringUtils.isBlank(field.getFieldLabel())) {
            return DataVo.failure("字段显示名不能为空");
        }
        if (modelFieldDao.checkFieldName(field.getModelId(), field.getFieldName())) {
            return DataVo.failure("字段名已存在");
        }
        field.setColumnType(type.resolveColumnType(field.getMaxlength()));
        field.setId(new SnowFlake(2, 3).nextId());
        field.setStatus(1);
        field.setCreateTime(new Date());
        if (modelFieldDao.addField(field) > 0) {
            modelTableService.addColumn(field.getModelId(), field);
            modelService.evictCache(model.getCode(), field.getModelId());
            return DataVo.success("字段已添加，数据列已生成");
        }
        return data;
    }

    /**
     * 编辑字段：field_name/field_type 不可改；类型变更仅允许 varchar 同组加长，其余拒绝。
     */
    public DataVo updateField(ModelField form) {
        ModelField old = modelFieldDao.findFieldById(form.getId());
        if (old == null) {
            return DataVo.failure("字段不存在");
        }
        FieldTypeEnum oldType = FieldTypeEnum.of(old.getFieldType());
        String oldColType = old.getColumnType();
        String newColType = oldColType;

        // varchar 加长兼容
        if (form.getMaxlength() != null && !form.getMaxlength().equals(old.getMaxlength())
                && oldType == FieldTypeEnum.INPUT) {
            newColType = oldType.resolveColumnType(form.getMaxlength());
            if (newColType.length() < oldColType.length()) {
                return DataVo.failure("varchar 仅允许加长，不允许缩短");
            }
        }

        form.setFieldName(old.getFieldName());
        form.setFieldType(old.getFieldType());
        form.setColumnType(newColType);
        if (modelFieldDao.updateField(form) > 0) {
            if (!newColType.equals(oldColType)) {
                modelTableService.modifyColumn(old.getModelId(), old.getFieldName(), newColType);
            }
            modelService.evictCache(null, old.getModelId());
            return DataVo.success("字段已更新");
        }
        return DataVo.failure("更新失败");
    }

    /**
     * 删除字段：DROP COLUMN + 删元数据
     */
    public DataVo deleteField(Long id) {
        ModelField field = modelFieldDao.findFieldById(id);
        if (field == null) {
            return DataVo.failure("字段不存在或已删除");
        }
        modelTableService.dropColumn(field.getModelId(), field.getFieldName());
        modelFieldDao.deleteFieldById(id);
        modelService.evictCache(null, field.getModelId());
        return DataVo.success("字段已删除，数据列已移除");
    }

    public void updateSort(Long id, int sort) {
        modelFieldDao.updateFieldSort(id, sort);
    }
}
