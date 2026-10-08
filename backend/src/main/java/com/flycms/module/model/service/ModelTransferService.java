package com.flycms.module.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.model.ModelField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * G20 模型导入导出（schema-as-code，对标 Payload config-as-code / Drupal CMI）。
 *
 * <p>导出 = 模型元数据 + 字段定义（含 GROUP/REPEATER 子字段，按 parentFieldName 关联）+
 * 分类树（按 fatherName 关联）的单个 JSON，可入 git、可跨环境同步。
 * 导入幂等：模型按 code 对齐（存在则更新元数据，不存在则新建——自动建表+生成骨架）；
 * 字段按 fieldName 对齐（存在则更新可变元数据，类型不可变；缺失则新增并建列）；
 * 分类按「同父下名称」对齐。relate_model 指向的外部模型缺失时报出依赖清单（先导依赖再导本模型）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelTransferService {

    private static final Logger log = LoggerFactory.getLogger(ModelTransferService.class);

    /** 导出/导入的字段元数据键（可变属性全集） */
    private static final List<String> FIELD_META_KEYS = List.of(
            "fieldName", "parentFieldName", "fieldLabel", "fieldType", "relateModel", "defaultValue",
            "maxlength", "options", "isRequired", "isList", "isSearch", "isFilter", "isForm",
            "regex", "placeholder", "tips", "isUnique", "minValue", "maxValue",
            "tabName", "sort", "visibleWhen", "rollupExpr", "lookupFields", "formula");

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelFieldService modelFieldService;
    @Autowired
    private ModelCategoryService modelCategoryService;
    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private com.flycms.module.model.dao.ComponentDao componentDao;

    // /////////////////// 导出 ///////////////////

    public Map<String, Object> exportModel(Long modelId) {
        Model model = modelService.findModelById(modelId);
        if (model == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", model.getName());
        meta.put("code", model.getCode());
        meta.put("titleLabel", model.getTitleLabel());
        meta.put("icon", model.getIcon());
        meta.put("description", model.getDescription());
        meta.put("useContent", model.getUseContent());
        meta.put("useSeo", model.getUseSeo());
        out.put("model", meta);

        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, null);
        Map<Long, String> nameById = new HashMap<>();
        for (ModelField f : fields) {
            nameById.put(f.getId(), f.getFieldName());
        }
        List<Map<String, Object>> fieldMaps = new ArrayList<>();
        for (ModelField f : fields) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("fieldName", f.getFieldName());
            fm.put("parentFieldName", f.getParentId() == null || f.getParentId() == 0 ? null : nameById.get(f.getParentId()));
            fm.put("fieldLabel", f.getFieldLabel());
            fm.put("fieldType", f.getFieldType());
            fm.put("relateModel", f.getRelateModel());
            fm.put("defaultValue", f.getDefaultValue());
            fm.put("maxlength", f.getMaxlength());
            fm.put("options", f.getOptions());
            fm.put("isRequired", f.getIsRequired());
            fm.put("isList", f.getIsList());
            fm.put("isSearch", f.getIsSearch());
            fm.put("isFilter", f.getIsFilter());
            fm.put("isForm", f.getIsForm());
            fm.put("regex", f.getRegex());
            fm.put("placeholder", f.getPlaceholder());
            fm.put("tips", f.getTips());
            fm.put("isUnique", f.getIsUnique());
            fm.put("minValue", f.getMinValue());
            fm.put("maxValue", f.getMaxValue());
            fm.put("tabName", f.getTabName());
            fm.put("sort", f.getSort());
            // 启用态也随导出往返：早前导出漏写该键 → 导入侧读不到 → 已存在字段被写 0（禁用）
            fm.put("status", f.getStatus());
            fm.put("visibleWhen", f.getVisibleWhen());
            fm.put("rollupExpr", f.getRollupExpr());
            fm.put("lookupFields", f.getLookupFields());
            fm.put("formula", f.getFormula());
            fieldMaps.add(fm);
        }
        out.put("fields", fieldMaps);

        List<ModelCategory> allCats = modelCategoryService.findCategoriesByModelId(modelId, null);
        Map<Long, String> catNameById = new HashMap<>();
        for (ModelCategory c : allCats) {
            catNameById.put(c.getId(), c.getName());
        }
        List<Map<String, Object>> catMaps = new ArrayList<>();
        for (ModelCategory c : allCats) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("name", c.getName());
            cm.put("fatherName", c.getFatherId() == null || c.getFatherId() == 0
                    ? null : catNameById.get(c.getFatherId()));
            cm.put("sort", c.getSort());
            cm.put("keywords", c.getKeywords());
            cm.put("description", c.getDescription());
            catMaps.add(cm);
        }
        out.put("categories", catMaps);
        return out;
    }

    // /////////////////// 导入 ///////////////////

    @SuppressWarnings("unchecked")
    public DataVo importModel(String payloadJson) {
        Map<String, Object> payload;
        try {
            payload = JSON.parseObject(payloadJson, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            return DataVo.failure("JSON 解析失败：" + e.getMessage());
        }
        if (payload == null || !(payload.get("model") instanceof Map)) {
            return DataVo.failure("缺少 model 定义");
        }
        Map<String, Object> meta = (Map<String, Object>) payload.get("model");
        String code = StringUtils.trimToEmpty((String) meta.get("code"));
        String name = StringUtils.trimToEmpty((String) meta.get("name"));
        try {
            SqlSafeUtil.safeModelCode(code);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("模型标识不合法：" + code);
        }
        if (name.isEmpty()) {
            return DataVo.failure("模型名称不能为空");
        }

        // ---- 模型：按 code 对齐（存在更新元数据 / 缺失新建） ----
        Model model = modelService.findModelByCode(code);
        boolean created = false;
        if (model == null) {
            Model m = new Model();
            m.setName(name);
            m.setCode(code);
            m.setTitleLabel(StringUtils.defaultIfBlank((String) meta.get("titleLabel"), "标题"));
            m.setIcon((String) meta.get("icon"));
            m.setDescription((String) meta.get("description"));
            // 预设包可声明模型级开关（enableSubmit/adminCreate）
            if (meta.get("enableSubmit") != null) {
                m.setEnableSubmit(Integer.parseInt(String.valueOf(meta.get("enableSubmit"))));
            }
            if (meta.get("adminCreate") != null) {
                m.setAdminCreate(Integer.parseInt(String.valueOf(meta.get("adminCreate"))));
            }
            if (meta.get("listTemplate") != null) {
                m.setListTemplate(StringUtils.trimToNull((String) meta.get("listTemplate")));
            }
            if (meta.get("detailTemplate") != null) {
                m.setDetailTemplate(StringUtils.trimToNull((String) meta.get("detailTemplate")));
            }
            DataVo vo = modelService.addModel(m);
            if (vo.getCode() != DataVo.CODE_SUCCESS) {
                return vo;
            }
            model = modelService.findModelByCode(code);
            created = true;
        } else {
            Model form = new Model();
            form.setId(model.getId());
            form.setName(name);
            form.setTitleLabel(StringUtils.defaultIfBlank((String) meta.get("titleLabel"), model.getTitleLabel()));
            form.setIcon((String) meta.get("icon"));
            form.setDescription((String) meta.get("description"));
            if (meta.get("useContent") != null) {
                form.setUseContent(Integer.parseInt(String.valueOf(meta.get("useContent"))));
            }
            if (meta.get("useSeo") != null) {
                form.setUseSeo(Integer.parseInt(String.valueOf(meta.get("useSeo"))));
            }
            if (meta.get("enableSubmit") != null) {
                form.setEnableSubmit(parseInt(meta.get("enableSubmit")));
            }
            if (meta.get("adminCreate") != null) {
                form.setAdminCreate(parseInt(meta.get("adminCreate")));
            }
            if (meta.containsKey("listTemplate")) {
                form.setListTemplate(StringUtils.trimToNull((String) meta.get("listTemplate")));
            }
            if (meta.containsKey("detailTemplate")) {
                form.setDetailTemplate(StringUtils.trimToNull((String) meta.get("detailTemplate")));
            }
            modelService.updateModel(form);
        }

        List<Map<String, Object>> jsonFields = castList(payload.get("fields"));
        Set<String> missingDeps = new HashSet<>();
        int fieldAdded = 0;
        int fieldUpdated = 0;
        int fieldFailed = 0;
        List<String> errors = new ArrayList<>();

        // 两遍：先顶层（建列），后子字段（parentId 依赖顶层就绪）
        List<ModelField> existing = modelFieldDao.findFieldsByModelId(model.getId(), null);
        Map<String, ModelField> existingByName = new HashMap<>();
        for (ModelField f : existing) {
            existingByName.put(f.getFieldName(), f);
        }
        Map<String, Long> importedTopIds = new HashMap<>();

        for (boolean children : new boolean[]{false, true}) {
            for (Map<String, Object> fm : jsonFields) {
                String parentName = trimToNull((String) fm.get("parentFieldName"));
                boolean isChild = parentName != null;
                if (isChild != children) {
                    continue;
                }
                ModelField form = fieldFromJson(fm, model.getCode());
                if (form == null) {
                    fieldFailed++;
                    errors.add("字段定义非法：" + fm.get("fieldName"));
                    continue;
                }
                form.setModelId(model.getId());
                // G20：外部模型依赖收集（relate/relates/user？——user 绑平台用户不依赖模型；
                // category 的绑定模型同理。仅 relate/relates 的外部 code 记入缺失清单）
                if (form.getRelateModel() != null && !form.getRelateModel().isBlank()
                        && !form.getRelateModel().equals(model.getCode())
                        && List.of("relate", "relates").contains(form.getFieldType())
                        && modelService.findModelByCode(form.getRelateModel()) == null) {
                    missingDeps.add(form.getRelateModel());
                }
                ModelField old = existingByName.get(form.getFieldName());
                if (isChild) {
                    Long parentId = importedTopIds.get(parentName);
                    if (parentId == null) {
                        ModelField p = existingByName.get(parentName);
                        parentId = p == null ? null : p.getId();
                    }
                    if (parentId == null) {
                        fieldFailed++;
                        errors.add("子字段 " + form.getFieldName() + " 的父字段 " + parentName + " 不存在");
                        continue;
                    }
                    form.setParentId(parentId);
                }
                if (old == null) {
                    DataVo vo = modelFieldService.addField(form);
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        fieldFailed++;
                        errors.add(form.getFieldName() + "：" + vo.getMessage());
                        continue;
                    }
                    fieldAdded++;
                    if (!isChild) {
                        ModelField now = modelFieldDao.findFieldsByModelId(model.getId(), null).stream()
                                .filter(f -> f.getFieldName().equals(form.getFieldName()))
                                .findFirst().orElse(null);
                        if (now != null) {
                            importedTopIds.put(form.getFieldName(), now.getId());
                            existingByName.put(form.getFieldName(), now);
                        }
                    }
                } else {
                    form.setId(old.getId());
                    DataVo vo = modelFieldService.updateField(form);
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        fieldFailed++;
                        errors.add(form.getFieldName() + "：" + vo.getMessage());
                        continue;
                    }
                    fieldUpdated++;
                }
            }
        }

        // ---- 分类：同父下按名称对齐 ----
        int catAdded = 0;
        List<Map<String, Object>> jsonCats = castList(payload.get("categories"));
        if (!jsonCats.isEmpty()) {
            List<ModelCategory> current = modelCategoryService.findCategoriesByModelId(model.getId(), null);
            Set<String> rootNames = new HashSet<>();
            for (ModelCategory c : current) {
                if (c.getFatherId() == null || c.getFatherId() == 0) {
                    rootNames.add(c.getName());
                }
            }
            Map<String, Long> nameId = new HashMap<>();
            for (Map<String, Object> cm : jsonCats) {
                String catName = trimToNull((String) cm.get("name"));
                if (catName == null) {
                    continue;
                }
                String fatherName = trimToNull((String) cm.get("fatherName"));
                if (fatherName == null) {
                    if (!rootNames.contains(catName)) {
                        ModelCategory c = new ModelCategory();
                        c.setModelId(model.getId());
                        c.setFatherId(0L);
                        c.setName(catName);
                        c.setSort(parseInt(cm.get("sort")));
                        c.setKeywords((String) cm.get("keywords"));
                        c.setDescription((String) cm.get("description"));
                        modelCategoryService.addCategory(c);
                        catAdded++;
                    }
                } else {
                    Long fatherId = nameId.get(fatherName);
                    if (fatherId == null) {
                        for (ModelCategory c : current) {
                            if (fatherName.equals(c.getName())) {
                                fatherId = c.getId();
                                break;
                            }
                        }
                    }
                    if (fatherId == null) {
                        continue;
                    }
                    ModelCategory c = new ModelCategory();
                    c.setModelId(model.getId());
                    c.setFatherId(fatherId);
                    c.setName(catName);
                    c.setSort(parseInt(cm.get("sort")));
                    c.setKeywords((String) cm.get("keywords"));
                    c.setDescription((String) cm.get("description"));
                    modelCategoryService.addCategory(c);
                    nameId.put(catName, c.getId());
                    catAdded++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelId", model.getId());
        result.put("modelCreated", created);
        result.put("fieldAdded", fieldAdded);
        result.put("fieldUpdated", fieldUpdated);
        result.put("fieldFailed", fieldFailed);
        result.put("categoryAdded", catAdded);
        result.put("errors", errors);
        if (!missingDeps.isEmpty()) {
            result.put("missingModelDeps", missingDeps);
        }
        return DataVo.success("导入完成", result);
    }

    // /////////////////// G15 字段组库（复制式 Component） ///////////////////

    public List<Map<String, Object>> listComponents() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (com.flycms.module.model.model.Component c : componentDao.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(c.getId()));
            m.put("code", c.getCode());
            m.put("name", c.getName());
            m.put("remark", c.getRemark());
            try {
                m.put("fields", JSON.parseArray(c.getFieldsJson()));
            } catch (Exception e) {
                m.put("fields", List.of());
            }
            m.put("fieldCount", ((List<?>) m.get("fields")).size());
            m.put("createTime", String.valueOf(c.getCreateTime()));
            out.add(m);
        }
        return out;
    }

    /** 新增/更新字段组（按 code upsert）；fields = 与模型导出同构的字段定义数组 */
    public DataVo saveComponent(String code, String name, String remark, String fieldsJson) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(name)) {
            return DataVo.failure("标识与名称不能为空");
        }
        try {
            SqlSafeUtil.safeColumnName(code);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("标识不合法（需为合法列名）：" + e.getMessage());
        }
        List<Map<String, Object>> fields;
        try {
            fields = JSON.parseObject(fieldsJson, new TypeReference<List<Map<String, Object>>>() { });
        } catch (Exception e) {
            return DataVo.failure("fields JSON 解析失败：" + e.getMessage());
        }
        if (fields == null || fields.isEmpty()) {
            return DataVo.failure("字段组至少要有一个子字段定义");
        }
        com.flycms.module.model.model.Component c = componentDao.findByCode(code);
        com.flycms.module.model.model.Component entity =
                new com.flycms.module.model.model.Component();
        entity.setCode(code);
        entity.setName(name);
        entity.setRemark(StringUtils.trimToEmpty(remark));
        entity.setFieldsJson(JSON.toJSONString(fields));
        if (c == null) {
            entity.setId(com.flycms.core.utils.SnowFlake.getInstance().nextId());
            componentDao.add(entity);
            return DataVo.success("字段组已入库");
        }
        entity.setId(c.getId());
        componentDao.update(entity);
        return DataVo.success("字段组已更新");
    }

    public DataVo deleteComponent(Long id) {
        componentDao.delete(id);
        return DataVo.success("已删除");
    }

    /**
     * 应用字段组到模型：新建 group 字段（fieldName=组件 code）+ 子字段逐个复制。
     * 同名子字段已存在则跳过（复制式语义：不做联动更新）。
     */
    public DataVo applyComponent(Long modelId, Long componentId) {
        Model model = modelService.findModelById(modelId);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        com.flycms.module.model.model.Component c = componentDao.findById(componentId);
        if (c == null) {
            return DataVo.failure("字段组不存在");
        }
        // 父字段：group 类型，fieldName=组件 code（冲突则加后缀）
        List<ModelField> existing = modelFieldDao.findFieldsByModelId(modelId, null);
        Set<String> taken = new HashSet<>();
        for (ModelField f : existing) {
            taken.add(f.getFieldName());
        }
        String groupFieldName = c.getCode();
        if (taken.contains(groupFieldName)) {
            groupFieldName = groupFieldName + "_copy";
            int n = 2;
            while (taken.contains(groupFieldName)) {
                groupFieldName = c.getCode() + "_copy" + (n++);
            }
        }
        String finalGroupName = groupFieldName;
        ModelField group = new ModelField();
        group.setModelId(modelId);
        group.setFieldName(groupFieldName);
        group.setFieldLabel(c.getName());
        group.setFieldType("group");
        group.setTabName("基础信息");
        group.setSort(60);
        group.setIsUnique(0);
        DataVo vo = modelFieldService.addField(group);
        if (vo.getCode() != DataVo.CODE_SUCCESS) {
            return vo;
        }
        ModelField groupNow = modelFieldDao.findFieldsByModelId(modelId, null).stream()
                .filter(f -> f.getFieldName().equals(finalGroupName)).findFirst().orElse(null);
        if (groupNow == null) {
            return DataVo.failure("父字段创建失败");
        }
        List<Map<String, Object>> children;
        try {
            children = JSON.parseObject(c.getFieldsJson(),
                    new TypeReference<List<Map<String, Object>>>() { });
        } catch (Exception e) {
            return DataVo.failure("字段组定义损坏：" + e.getMessage());
        }
        int copied = 0;
        List<String> skipped = new ArrayList<>();
        for (Map<String, Object> fm : children) {
            ModelField child = fieldFromJson(fm, model.getCode());
            if (child == null) {
                skipped.add(String.valueOf(fm.get("fieldName")) + "(定义非法)");
                continue;
            }
            if (taken.contains(child.getFieldName())) {
                skipped.add(child.getFieldName() + "(已存在)");
                continue;
            }
            child.setModelId(modelId);
            child.setParentId(groupNow.getId());
            DataVo r = modelFieldService.addField(child);
            if (r.getCode() == DataVo.CODE_SUCCESS) {
                copied++;
                taken.add(child.getFieldName());
            } else {
                skipped.add(child.getFieldName() + "(" + r.getMessage() + ")");
            }
        }
        return DataVo.success("已应用字段组：父字段 " + finalGroupName + "，复制子字段 " + copied
                + " 个" + (skipped.isEmpty() ? "" : "（跳过已存在：" + String.join(",", skipped) + "）"));
    }

    /** 存字段组：把模型里某个 group/repeater 字段（含其子字段）入库为组件 */
    public DataVo pickupComponent(Long modelId, Long fieldId, String code, String name, String remark) {
        ModelField group = modelFieldDao.findFieldById(fieldId);
        if (group == null || (group.getParentId() != null && group.getParentId() > 0)) {
            return DataVo.failure("父字段不存在");
        }
        List<ModelField> all = modelFieldDao.findFieldsByModelId(modelId, null);
        List<Map<String, Object>> defs = new ArrayList<>();
        for (ModelField f : all) {
            if (fieldId.equals(f.getParentId())) {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("fieldName", f.getFieldName());
                fm.put("fieldLabel", f.getFieldLabel());
                fm.put("fieldType", f.getFieldType());
                fm.put("options", f.getOptions());
                fm.put("maxlength", f.getMaxlength());
                fm.put("isRequired", f.getIsRequired());
                fm.put("regex", f.getRegex());
                fm.put("placeholder", f.getPlaceholder());
                fm.put("tips", f.getTips());
                fm.put("formula", f.getFormula());
                defs.add(fm);
            }
        }
        if (defs.isEmpty()) {
            return DataVo.failure("该字段组还没有子字段，无可入库内容");
        }
        return saveComponent(code, name, remark, JSON.toJSONString(defs));
    }

    /** JSON 字段定义 → ModelField（可变属性逐键回填；fieldName/fieldType 基础校验） */
    private ModelField fieldFromJson(Map<String, Object> fm, String selfCode) {
        String fieldName = trimToNull((String) fm.get("fieldName"));
        String fieldType = trimToNull((String) fm.get("fieldType"));
        if (fieldName == null || fieldType == null) {
            return null;
        }
        try {
            SqlSafeUtil.safeColumnName(fieldName);
            com.flycms.module.model.enums.FieldTypeEnum.of(fieldType);
        } catch (IllegalArgumentException e) {
            return null;
        }
        ModelField f = new ModelField();
        f.setFieldName(fieldName);
        f.setFieldType(fieldType);
        f.setFieldLabel(StringUtils.defaultIfBlank((String) fm.get("fieldLabel"), fieldName));
        for (String key : List.of("defaultValue", "options", "regex", "placeholder", "tips",
                "visibleWhen", "rollupExpr", "lookupFields", "relateModel")) {
            if (fm.containsKey(key)) {
                assign(f, key, trimToNull((String) fm.get(key)));
            }
        }
        for (String key : List.of("maxlength", "isRequired", "isList", "isSearch", "isFilter",
                "isUnique", "sort", "isForm")) {
            if (fm.get(key) != null) {
                assignInt(f, key, parseInt(fm.get(key)));
            }
        }
        if (fm.get("minValue") != null) {
            f.setMinValue(new java.math.BigDecimal(String.valueOf(fm.get("minValue"))));
        }
        if (fm.get("maxValue") != null) {
            f.setMaxValue(new java.math.BigDecimal(String.valueOf(fm.get("maxValue"))));
        }
        f.setTabName(StringUtils.defaultIfBlank((String) fm.get("tabName"), "基础信息"));
        if (f.getSort() == 0) {
            f.setSort(50);
        }
        // is_unique 列 NOT NULL——JSON 未带时补 0（导入/字段组应用的子字段定义常缺该键）
        if (f.getIsUnique() == null) {
            f.setIsUnique(0);
        }
        // status 未声明时补 1（启用）。ModelField.status 是原始 int，缺省会落成 0=禁用；
        // 预设包的字段定义全部不写 status，早前因此让「已存在字段」在重新导入后被整批禁用
        // （见 sql/migrations/2026-10-08-model-field-status.sql 的数据修复）。
        f.setStatus(fm.get("status") != null ? java.util.Objects.requireNonNullElse(parseInt(fm.get("status")), 1) : 1);
        return f;
    }

    private void assign(ModelField f, String key, String value) {
        switch (key) {
            case "defaultValue" -> f.setDefaultValue(value);
            case "options" -> f.setOptions(value);
            case "regex" -> f.setRegex(value);
            case "placeholder" -> f.setPlaceholder(value);
            case "tips" -> f.setTips(value);
            case "visibleWhen" -> f.setVisibleWhen(value);
            case "rollupExpr" -> f.setRollupExpr(value);
            case "lookupFields" -> f.setLookupFields(value);
            case "formula" -> f.setFormula(value);
            case "relateModel" -> f.setRelateModel(value);
            default -> {
                // 不可达：键集合由 FIELD_META_KEYS 约定
            }
        }
    }

    private void assignInt(ModelField f, String key, Integer value) {
        if (value == null) {
            return;
        }
        switch (key) {
            case "maxlength" -> f.setMaxlength(value);
            case "isRequired" -> f.setIsRequired(value);
            case "isList" -> f.setIsList(value);
            case "isSearch" -> f.setIsSearch(value);
            case "isFilter" -> f.setIsFilter(value);
            case "isForm" -> f.setIsForm(value);
            case "isUnique" -> f.setIsUnique(value);
            case "sort" -> f.setSort(value);
            default -> {
                // 不可达
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object item : (List<Object>) o) {
                if (item instanceof Map) {
                    out.add((Map<String, Object>) item);
                }
            }
        }
        return out;
    }

    private Integer parseInt(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimToNull(String v) {
        return StringUtils.trimToNull(v);
    }
}
