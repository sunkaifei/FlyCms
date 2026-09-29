package com.flycms.module.model.service;

import com.flycms.core.entity.DataVo;
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
import java.util.Set;

/**
 * 模型字段元数据服务：字段增删改 + 类型兼容校验 + DDL 联动。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelFieldService {

    /** visible_when 操作符白名单（P1 条件显隐） */
    private static final Set<String> VISIBLE_OPS = Set.of("eq", "neq", "in", "notin", "empty", "notempty");
    /** rollup 聚合函数白名单（P2） */
    private static final Set<String> ROLLUP_FUNCS = Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

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
        // E1：关联字段必须显式指定目标模型（关联本模型时填本模型 code，用于自关联树）
        if (type.isRelation()) {
            if (StringUtils.isBlank(field.getRelateModel())) {
                return DataVo.failure("请选择被引用模型（关联本模型请选择「本模型」）");
            }
            String target = field.getRelateModel().trim();
            if (!target.equals(model.getCode()) && modelService.findModelByCode(target) == null) {
                return DataVo.failure("被引用模型不存在：" + target);
            }
            field.setRelateModel(target);
        } else {
            field.setRelateModel(null);
        }
        if (modelFieldDao.checkFieldName(field.getModelId(), field.getFieldName())) {
            return DataVo.failure("字段名已存在");
        }
        // P1 子字段：parentId>0 时校验父字段为 GROUP/REPEATER 且同模型，子字段不建物理列
        if (field.getParentId() != null && field.getParentId() > 0) {
            ModelField parent = modelFieldDao.findFieldById(field.getParentId());
            if (parent == null || parent.getModelId() != field.getModelId()) {
                return DataVo.failure("父字段不存在或不属于当前模型");
            }
            if (!FieldTypeEnum.of(parent.getFieldType()).isStructure()) {
                return DataVo.failure("只有字段组(group)/重复行(repeater)可以包含子字段");
            }
            if (!type.canBeStructureChild()) {
                return DataVo.failure("该类型不能作为子字段");
            }
        } else {
            field.setParentId(0L);
        }
        // P2 Rollup：虚拟字段，校验 rollup_expr 结构
        if (type == FieldTypeEnum.ROLLUP) {
            DataVo rollup = validateRollupExpr(field, model);
            if (rollup.getCode() != DataVo.CODE_SUCCESS) {
                return rollup;
            }
        } else {
            field.setRollupExpr(null);
        }
        // E6 公式字段：表达式必填且仅允许 数字/字段名/四则/括号/空格（受限 SpEL 求值的安全前提）
        if (type == FieldTypeEnum.FORMULA) {
            String formulaExpr = StringUtils.trimToEmpty(field.getFormula());
            if (formulaExpr.isEmpty()) {
                return DataVo.failure("公式不能为空（如 price * 0.88）");
            }
            if (!formulaExpr.matches("^[a-z0-9_ +*/().-]+$")) {
                return DataVo.failure("公式仅允许字段名、数字与 + - * / ( ) 运算");
            }
            field.setFormula(formulaExpr);
        } else {
            field.setFormula(null);
        }
        // P1 条件显隐：结构化校验（目标字段须为本模型顶层字段）
        DataVo vis = validateVisibleWhen(field, model, null);
        if (vis.getCode() != DataVo.CODE_SUCCESS) {
            return vis;
        }
        field.setColumnType(type.resolveColumnType(field.getMaxlength()));
        field.setStatus(1);
        field.setCreateTime(new Date());
        if (modelFieldDao.addField(field) > 0) {
            // 子字段/虚拟字段不建物理列（rollup/m2a 无列，结构子字段存父字段 JSON 内）
            if ((field.getParentId() == null || field.getParentId() == 0) && !type.isVirtual()) {
                modelTableService.addColumn(model.getCode(), field);
            }
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
        // E6：formula 未传沿用原值（update XML 无条件更新该列）；非 FORMULA 类型一律清除
        if (form.getFormula() == null) {
            form.setFormula(old.getFormula());
        }
        if (old.getFieldType() != null
                && com.flycms.module.model.enums.FieldTypeEnum.of(old.getFieldType())
                        != com.flycms.module.model.enums.FieldTypeEnum.FORMULA) {
            form.setFormula(null);
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
        Model owner = modelService.findModelById(old.getModelId());
        if (owner == null) {
            return DataVo.failure("模型不存在");
        }
        // E1：类型不可改，故关联目标只能在「本就是关联字段」时维护；
        // 未传时沿用原值（避免 XML 的 relate_model = #{relateModel} 把它清空）
        if (oldType.isRelation()) {
            if (StringUtils.isBlank(form.getRelateModel())) {
                form.setRelateModel(old.getRelateModel());
            } else {
                String target = form.getRelateModel().trim();
                if (!target.equals(owner.getCode()) && modelService.findModelByCode(target) == null) {
                    return DataVo.failure("被引用模型不存在：" + target);
                }
                form.setRelateModel(target);
            }
        } else {
            form.setRelateModel(null);
        }
        // P1 条件显隐：未传沿用原值；传了做结构校验
        if (form.getVisibleWhen() == null) {
            form.setVisibleWhen(old.getVisibleWhen());
        } else {
            DataVo vis = validateVisibleWhen(form, owner, old.getId());
            if (vis.getCode() != DataVo.CODE_SUCCESS) {
                return vis;
            }
        }
        // P2 Rollup：类型不可改，表达式只在本就是 rollup 字段时可维护
        if (oldType == FieldTypeEnum.ROLLUP) {
            if (form.getRollupExpr() == null) {
                form.setRollupExpr(old.getRollupExpr());
            } else {
                form.setModelId(old.getModelId());
                DataVo rollup = validateRollupExpr(form, owner);
                if (rollup.getCode() != DataVo.CODE_SUCCESS) {
                    return rollup;
                }
            }
        } else {
            form.setRollupExpr(null);
        }
        if (modelFieldDao.updateField(form) > 0) {
            // 虚拟字段（formula/rollup/m2a）无列定义（columnType 为 NULL），无物理列可改
            if (newColType != null && !newColType.equals(oldColType)) {
                modelTableService.modifyColumn(owner.getCode(), old.getFieldName(), newColType);
            }
            modelService.evictCache(null, old.getModelId());
            return DataVo.success("字段已更新");
        }
        return DataVo.failure("更新失败");
    }

    /**
     * 删除字段：DROP COLUMN + 删元数据。GROUP/REPEATER 级联删除子字段元数据（结构存父字段 JSON 列内）。
     */
    public DataVo deleteField(Long id) {
        ModelField field = modelFieldDao.findFieldById(id);
        if (field == null) {
            return DataVo.failure("字段不存在或已删除");
        }
        Model owner = modelService.findModelById(field.getModelId());
        if (owner == null) {
            return DataVo.failure("模型不存在");
        }
        FieldTypeEnum type = FieldTypeEnum.of(field.getFieldType());
        // 子字段不占物理列；虚拟字段（rollup/m2a）也无列
        if ((field.getParentId() == null || field.getParentId() == 0) && type.hasColumn()) {
            modelTableService.dropColumn(owner.getCode(), field.getFieldName());
        }
        if (type.isStructure()) {
            modelFieldDao.deleteFieldsByParentId(field.getId());
        }
        modelFieldDao.deleteFieldById(id);
        modelService.evictCache(null, field.getModelId());
        return DataVo.success("字段已删除，数据列已移除");
    }

    // /////////////////// P1/P2 字段定义校验 ///////////////////

    /**
     * P2 Rollup 表达式校验：{"source":"关联字段名","func":"COUNT|SUM|AVG|MIN|MAX","column":"数值列"}。
     * source 必须是本模型的单值 relate 字段（rollup v1 只聚合 RELATE 指向的目标行）；
     * func 非 COUNT 时 column 必填且过列名白名单。
     */
    private DataVo validateRollupExpr(ModelField field, Model model) {
        java.util.Map<String, Object> expr;
        try {
            expr = com.alibaba.fastjson2.JSON.parseObject(field.getRollupExpr());
        } catch (Exception e) {
            return DataVo.failure("rollup_expr 必须是 JSON 对象");
        }
        if (expr == null) {
            return DataVo.failure("rollup_expr 不能为空");
        }
        String source = String.valueOf(expr.get("source"));
        String func = String.valueOf(expr.get("func"));
        String column = expr.get("column") == null ? null : String.valueOf(expr.get("column"));
        ModelField src = null;
        for (ModelField f : modelFieldDao.findFieldsByModelId(field.getModelId(), 1)) {
            if (f.getFieldName().equals(source)) {
                src = f;
                break;
            }
        }
        if (src == null || !"relate".equals(src.getFieldType())) {
            return DataVo.failure("source 必须是本模型的单值关联字段（relate）：" + source);
        }
        if (!ROLLUP_FUNCS.contains(func)) {
            return DataVo.failure("func 只允许 COUNT/SUM/AVG/MIN/MAX：" + func);
        }
        if (!"COUNT".equals(func)) {
            if (column == null || column.isBlank()) {
                return DataVo.failure(func + " 聚合必须指定目标列 column");
            }
            try {
                SqlSafeUtil.safeColumnName(column);
            } catch (IllegalArgumentException e) {
                return DataVo.failure("聚合列非法：" + column);
            }
        }
        // 归一化回写
        java.util.Map<String, Object> normalized = new java.util.LinkedHashMap<>();
        normalized.put("source", source);
        normalized.put("func", func);
        if (!"COUNT".equals(func)) {
            normalized.put("column", column);
        }
        field.setRollupExpr(com.alibaba.fastjson2.JSON.toJSONString(normalized));
        return DataVo.success("ok");
    }

    /**
     * P1 条件显隐表达式校验：{"field":"字段名","op":"eq|neq|in|notin|empty|notempty","value":"比较值"}。
     * 目标字段必须是同模型字段（更新场景排除自身）；值统一按字符串比较。
     */
    private DataVo validateVisibleWhen(ModelField field, Model model, Long excludeFieldId) {
        if (StringUtils.isBlank(field.getVisibleWhen())) {
            field.setVisibleWhen(null);
            return DataVo.success("ok");
        }
        java.util.Map<String, Object> cond;
        try {
            cond = com.alibaba.fastjson2.JSON.parseObject(field.getVisibleWhen());
        } catch (Exception e) {
            return DataVo.failure("visible_when 必须是 JSON 对象");
        }
        String refField = String.valueOf(cond.get("field"));
        String op = String.valueOf(cond.get("op"));
        if (refField == null || refField.isBlank() || "null".equals(refField)) {
            return DataVo.failure("visible_when 缺少 field");
        }
        if (!VISIBLE_OPS.contains(op)) {
            return DataVo.failure("visible_when 的 op 只允许 eq/neq/in/notin/empty/notempty：" + op);
        }
        boolean refExists = false;
        for (ModelField f : modelFieldDao.findFieldsByModelId(field.getModelId(), 1)) {
            if (f.getFieldName().equals(refField)
                    && (excludeFieldId == null || excludeFieldId != f.getId())) {
                refExists = true;
                break;
            }
        }
        if (!refExists) {
            return DataVo.failure("visible_when 引用的字段不存在：" + refField);
        }
        return DataVo.success("ok");
    }

    public void updateSort(Long id, int sort) {
        modelFieldDao.updateFieldSort(id, sort);
    }
}
