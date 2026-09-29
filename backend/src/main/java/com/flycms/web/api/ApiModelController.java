package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.model.ModelField;
import com.flycms.module.model.service.ModelCategoryService;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelFieldService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.model.service.ModelTableService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义模型系统 REST 接口（模型/字段/内容/分类/附件 + 混合菜单动态路由）。
 *
 * 权限码 = action_key 原样（/api/system/model/** 等，见 sql/custom-model.sql）；
 * 未登录由 requireAdmin 抛 HTTP 401，无权限 requirePermission 抛 403。
 * 前端表单 POST 统一 form-urlencoded（@RequestParam 读取）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiModelController extends ApiBaseController {

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelFieldService modelFieldService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelCategoryService modelCategoryService;
    @Autowired
    private ModelTableService modelTableService;
    @Autowired
    private com.flycms.module.model.dao.ModelDataDao modelDataDao;
    @Autowired
    private com.flycms.module.admin.dao.PermissionDao permissionDao;

    /** 模型内容菜单挂在「内容」目录（/modelData）下 */
    private static final long MODEL_MENU_PARENT_ID = 900120L;
    private static final String MODEL_MENU_COMPONENT = "/system/modeldata/list";

    // /////////////////// 模型内容菜单权限节点同步 ///////////////////

    /**
     * 模型 CRUD 后同步 fly_admin_permission 节点（万能建模宗旨：在线建模的模型
     * 自动获得菜单与按模型粒度的权限设置，无需手工在菜单管理里补录）。
     *
     * <p>锚点约定：C 菜单 actionKey = /api/system/modelData/list@{modelId}（授权即
     * 可见+可读），F 按钮 actionKey = /api/system/modelData/save@{modelId}（发布）。
     * 通用节点 900006（/api/system/modelData/*）的尾星号仍匹配两种锚点，旧授权平滑兼容。
     */
    private void syncModelMenuNodesOnSave(com.flycms.module.model.model.Model model) {
        String listKey = modelDataListKey(model.getId());
        String saveKey = modelDataSaveKey(model.getId());
        // 幂等：已存在（如迁移脚本预置）则只同步名称/图标/显隐
        for (com.flycms.module.admin.model.Permission p : permissionDao.findPermissionByActionKey(listKey)) {
            if (p.getParentId() != null && p.getParentId() == MODEL_MENU_PARENT_ID) {
                updateModelMenuRow(p, model, 1);
                return;
            }
        }
        com.flycms.module.admin.model.Permission menu = new com.flycms.module.admin.model.Permission();
        menu.setId(new com.flycms.core.utils.SnowFlake(2, 3).nextId());
        menu.setParentId(MODEL_MENU_PARENT_ID);
        menu.setMenuType("C");
        menu.setMenuName(model.getName() + "管理");
        menu.setActionKey(listKey);
        menu.setPath("/modelData/" + model.getCode());
        menu.setComponent(MODEL_MENU_COMPONENT);
        menu.setIcon(StringUtils.defaultIfBlank(model.getIcon(), "lucide:file-text"));
        menu.setSort(model.getId() == null ? 99 : model.getId().intValue());
        menu.setVisible(1);
        permissionDao.addPermission(menu);

        com.flycms.module.admin.model.Permission btn = new com.flycms.module.admin.model.Permission();
        btn.setId(new com.flycms.core.utils.SnowFlake(2, 3).nextId());
        btn.setParentId(menu.getId());
        btn.setMenuType("F");
        btn.setMenuName("内容发布");
        btn.setActionKey(saveKey);
        btn.setVisible(1);
        permissionDao.addPermission(btn);
    }

    private void syncModelMenuNodesOnUpdate(com.flycms.module.model.model.Model model) {
        String listKey = modelDataListKey(model.getId());
        for (com.flycms.module.admin.model.Permission p : permissionDao.findPermissionByActionKey(listKey)) {
            if (p.getParentId() != null && p.getParentId() == MODEL_MENU_PARENT_ID) {
                // 模型停用 → 菜单隐藏（hideInMenu，路由仍注册）；启用 → 恢复
                updateModelMenuRow(p, model, model.getStatus() == 0 ? 0 : 1);
            }
        }
    }

    private void syncModelMenuNodesOnDelete(Long modelId) {
        String listKey = modelDataListKey(modelId);
        for (com.flycms.module.admin.model.Permission p : permissionDao.findPermissionByActionKey(listKey)) {
            if (p.getParentId() == null || p.getParentId() != MODEL_MENU_PARENT_ID) {
                continue;
            }
            for (com.flycms.module.admin.model.Permission child : permissionDao.findMenuChildren(p.getId())) {
                permissionDao.deletePermission(child.getId());
                permissionDao.deleteRolePermission(child.getId());
            }
            permissionDao.deletePermission(p.getId());
            permissionDao.deleteRolePermission(p.getId());
        }
    }

    private void updateModelMenuRow(com.flycms.module.admin.model.Permission node,
                                    com.flycms.module.model.model.Model model, int visible) {
        com.flycms.module.admin.model.Permission row = new com.flycms.module.admin.model.Permission();
        row.setId(node.getId());
        // update 接口 name/icon 可不传：为空时保持节点原值，避免覆盖成 "null管理"
        if (StringUtils.isNotBlank(model.getName())) {
            row.setMenuName(model.getName() + "管理");
        }
        if (StringUtils.isNotBlank(model.getIcon())) {
            row.setIcon(model.getIcon());
        }
        row.setVisible(visible);
        permissionDao.updateMenuRow(row);
    }

    private String modelDataListKey(Long modelId) {
        return "/api/system/modelData/list@" + modelId;
    }

    private String modelDataSaveKey(Long modelId) {
        return "/api/system/modelData/save@" + modelId;
    }

    // /////////////////// 模型定义 ///////////////////

    @ResponseBody
    @GetMapping("/system/model/list")
    public DataVo modelList(@RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/api/system/model/list");
        PageVo<Model> pageVo = modelService.getModelListPage(pageNum, 50);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/model/{id}")
    public DataVo modelDetail(@PathVariable Long id) {
        requirePermission("/api/system/model/list");
        Model model = modelService.findModelById(id);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        return DataVo.success("操作成功", model);
    }

    @ResponseBody
    @GetMapping("/system/model/byCode/{code}")
    public DataVo modelByCode(@PathVariable String code) {
        requirePermission("/api/system/model/list");
        Model model = modelService.findModelByCode(code);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        return DataVo.success("操作成功", model);
    }

    @ResponseBody
    @PostMapping("/system/model/save")
    public DataVo modelSave(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/model/save");
        Model model = new Model();
        model.setName(params.get("name"));
        model.setCode(params.get("code"));
        model.setTitleLabel(StringUtils.defaultIfBlank(params.get("titleLabel"), "标题"));
        model.setIcon(params.get("icon"));
        model.setDescription(params.get("description"));
        model.setSort(parseInt(params.get("sort"), 0));
        DataVo vo = modelService.addModel(model);
        if (vo.getCode() == DataVo.CODE_SUCCESS && model.getId() != null) {
            syncModelMenuNodesOnSave(model);
        }
        return vo;
    }

    @ResponseBody
    @PostMapping("/system/model/update")
    public DataVo modelUpdate(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/model/update");
        Long id = parseLong(params.get("id"));
        if (id == null) {
            return DataVo.failure("参数传递错误");
        }
        Model model = new Model();
        model.setId(id);
        model.setName(params.get("name"));
        model.setTitleLabel(params.get("titleLabel"));
        model.setListTemplate(params.get("listTemplate"));
        model.setDetailTemplate(params.get("detailTemplate"));
        model.setIcon(params.get("icon"));
        model.setDescription(params.get("description"));
        model.setSort(parseInt(params.get("sort"), 0));
        model.setStatus(parseInt(params.get("status"), 1));
        // U3 表单布局：基础选项卡开关（未传 = 不变更，避免部分更新回翻）
        if (params.containsKey("useContent")) {
            model.setUseContent(parseInt(params.get("useContent"), 1));
        }
        if (params.containsKey("useSeo")) {
            model.setUseSeo(parseInt(params.get("useSeo"), 1));
        }
        if (params.containsKey("enableComment")) {
            model.setEnableComment(parseInt(params.get("enableComment"), 1));
        }
        DataVo vo = modelService.updateModel(model);
        if (vo.getCode() == DataVo.CODE_SUCCESS) {
            syncModelMenuNodesOnUpdate(model);
        }
        return vo;
    }

    @ResponseBody
    @PostMapping("/system/model/del")
    public DataVo modelDel(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/model/del");
        DataVo vo = modelService.deleteModel(id);
        if (vo.getCode() == DataVo.CODE_SUCCESS) {
            syncModelMenuNodesOnDelete(id);
        }
        return vo;
    }

    // /////////////////// 模型字段 ///////////////////

    @ResponseBody
    @GetMapping("/system/modelField/list/{modelId}")
    public DataVo fieldList(@PathVariable Long modelId) {
        requirePermission("/api/system/modelField/list");
        return DataVo.success("操作成功", modelFieldService.findFieldsByModelId(modelId, null));
    }

    @ResponseBody
    @PostMapping("/system/modelField/save")
    public DataVo fieldSave(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelField/save");
        ModelField field = new ModelField();
        field.setModelId(parseLong(params.get("modelId")));
        field.setFieldName(params.get("fieldName"));
        field.setFieldLabel(params.get("fieldLabel"));
        field.setFieldType(params.get("fieldType"));
        field.setMaxlength(parseInteger(params.get("maxlength")));
        field.setOptions(params.get("options"));
        field.setIsRequired(parseInt(params.get("isRequired"), 0));
        field.setIsList(parseInt(params.get("isList"), 1));
        field.setIsSearch(parseInt(params.get("isSearch"), 0));
        field.setIsFilter(parseInt(params.get("isFilter"), 0));
        // U3 表单布局：表单隐藏（0=表单不渲染不提交不校验）
        field.setIsForm(parseInt(params.get("isForm"), 1));
        field.setRegex(params.get("regex"));
        field.setPlaceholder(params.get("placeholder"));
        field.setTips(params.get("tips"));
        field.setRelateModel(params.get("relateModel"));
        field.setIsUnique(parseInt(params.get("isUnique"), 0));
        field.setMinValue(parseDecimal(params.get("minValue")));
        field.setMaxValue(parseDecimal(params.get("maxValue")));
        // P1 结构层：父字段（子字段）与条件显隐
        field.setParentId(parseLong(params.get("parentId")));
        field.setVisibleWhen(blankToNull(params.get("visibleWhen")));
        // P2 关系层：Rollup 聚合表达式与 Lookup 展示列
        field.setRollupExpr(blankToNull(params.get("rollupExpr")));
        field.setLookupFields(blankToNull(params.get("lookupFields")));
        // E6 公式字段
        field.setFormula(blankToNull(params.get("formula")));
        field.setTabName(StringUtils.defaultIfBlank(params.get("tabName"), "基础信息"));
        field.setSort(parseInt(params.get("sort"), 0));
        if (field.getModelId() == null) {
            return DataVo.failure("参数传递错误");
        }
        return modelFieldService.addField(field);
    }

    @ResponseBody
    @PostMapping("/system/modelField/update")
    public DataVo fieldUpdate(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelField/save");
        Long id = parseLong(params.get("id"));
        if (id == null) {
            return DataVo.failure("参数传递错误");
        }
        ModelField field = new ModelField();
        field.setId(id);
        field.setFieldLabel(params.get("fieldLabel"));
        field.setMaxlength(parseInteger(params.get("maxlength")));
        field.setOptions(params.get("options"));
        field.setIsRequired(parseInt(params.get("isRequired"), 0));
        field.setIsList(parseInt(params.get("isList"), 1));
        field.setIsSearch(parseInt(params.get("isSearch"), 0));
        field.setIsFilter(parseInt(params.get("isFilter"), 0));
        // U3 表单布局：表单隐藏（0=表单不渲染不提交不校验）
        field.setIsForm(parseInt(params.get("isForm"), 1));
        field.setRegex(params.get("regex"));
        field.setPlaceholder(params.get("placeholder"));
        field.setTips(params.get("tips"));
        field.setRelateModel(params.get("relateModel"));
        field.setIsUnique(parseInt(params.get("isUnique"), 0));
        field.setMinValue(parseDecimal(params.get("minValue")));
        field.setMaxValue(parseDecimal(params.get("maxValue")));
        // P1/P2：visibleWhen/rollupExpr/lookupFields 未传（null）沿用原值；传空串 = 清除（服务端裁决）
        field.setVisibleWhen(params.containsKey("visibleWhen") ? params.get("visibleWhen") : null);
        field.setRollupExpr(params.containsKey("rollupExpr") ? params.get("rollupExpr") : null);
        field.setLookupFields(params.containsKey("lookupFields") ? blankToNull(params.get("lookupFields")) : null);
        field.setFormula(params.containsKey("formula") ? blankToNull(params.get("formula")) : null);
        field.setTabName(StringUtils.defaultIfBlank(params.get("tabName"), "基础信息"));
        field.setSort(parseInt(params.get("sort"), 0));
        field.setStatus(parseInt(params.get("status"), 1));
        return modelFieldService.updateField(field);
    }

    @ResponseBody
    @PostMapping("/system/modelField/del")
    public DataVo fieldDel(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/modelField/save");
        return modelFieldService.deleteField(id);
    }

    @ResponseBody
    @PostMapping("/system/modelField/sort")
    public DataVo fieldSort(@RequestParam(value = "ids", required = false) List<Long> ids,
                            @RequestParam(value = "sorts", required = false) List<Integer> sorts) {
        requirePermission("/api/system/modelField/save");
        if (ids == null || sorts == null || ids.size() != sorts.size()) {
            return DataVo.failure("参数传递错误");
        }
        for (int i = 0; i < ids.size(); i++) {
            modelFieldService.updateSort(ids.get(i), sorts.get(i));
        }
        return DataVo.success("排序已保存");
    }

    // /////////////////// 模型内容（动态表 CRUD） ///////////////////

    @ResponseBody
    @GetMapping("/system/modelData/list/{modelId}")
    public DataVo dataList(@PathVariable Long modelId,
                           @RequestParam(value = "p", defaultValue = "1") int pageNum,
                           @RequestParam(value = "rows", defaultValue = "20") int rows,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "status", required = false) Integer status,
                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                           @RequestParam(value = "orderby", required = false) String orderby,
                           @RequestParam(value = "order", required = false) String order,
                           @RequestParam Map<String, String> allParams) {
        // 按模型粒度鉴权：/api/system/modelData/list@{modelId}（通用节点 900006 尾星号仍兼容）
        requirePermission("/api/system/modelData/list@" + modelId);
        Map<String, String> filters = new HashMap<>(allParams);
        filters.remove("p");
        filters.remove("rows");
        filters.remove("title");
        filters.remove("status");
        filters.remove("categoryId");
        filters.remove("orderby");
        filters.remove("order");
        // E8 时间窗参数独立提取（不走自定义字段筛选）
        String timeField = filters.remove("timeField");
        String timeFrom = filters.remove("timeFrom");
        String timeTo = filters.remove("timeTo");
        // rows 由调用方指定（关联选择器需一次取更多候选），上限 200 防滥用
        int pageSize = Math.min(Math.max(rows, 1), 200);
        PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
                modelId, title, categoryId, status, filters, orderby, order, pageNum, pageSize, null,
                false, null, timeField, timeFrom, timeTo);
        // 附件/关联展开（E1）：后台列表也能显示关联内容名而非裸 id
        modelDataService.expandAttachments(modelId, pageVo.getList());
        // G14 字段级权限：无权字段连同展开键一并剔除
        List<com.flycms.module.model.model.ModelField> allFields =
                modelFieldService.findFieldsByModelId(modelId, null);
        for (Map<String, Object> row : pageVo.getList()) {
            stripRowByPermission(modelId, row, allFields);
        }
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/modelData/formMeta/{modelId}")
    public DataVo formMeta(@PathVariable Long modelId) {
        requirePermission("/api/system/modelData/list@" + modelId);
        Map<String, Object> meta = modelDataService.formMeta(modelId);
        // G14 字段级权限：无权字段直接从表单元数据剔除（前端不渲染即不提交）
        Object fieldsObj = meta.get("fields");
        if (fieldsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<com.flycms.module.model.model.ModelField> fields = (List) fieldsObj;
            meta.put("fields", filterFieldsByPermission(modelId, fields));
        }
        return DataVo.success("操作成功", meta);
    }

    @ResponseBody
    @GetMapping("/system/modelData/detail/{modelId}/{id}")
    public DataVo dataDetail(@PathVariable Long modelId, @PathVariable Long id) {
        requirePermission("/api/system/modelData/list@" + modelId);
        Map<String, Object> row = modelDataService.findDataById(modelId, id);
        if (row == null) {
            return DataVo.failure("内容不存在");
        }
        // 附件/关联展开（E1）：编辑弹窗需要 {field}Obj / {field}List 做回显
        modelDataService.expandAttachments(modelId, java.util.Collections.singletonList(row));
        // G14 字段级权限：无权字段连同展开键一并剔除
        stripRowByPermission(modelId, row, modelFieldService.findFieldsByModelId(modelId, null));
        return DataVo.success("操作成功", row);
    }

    @ResponseBody
    @PostMapping("/system/modelData/save")
    public DataVo dataSave(@RequestParam Map<String, String> params) {
        // 先解析 modelId 再鉴权：未登录走 requireAdmin 401，不破坏 401 口径
        Long modelId = parseLong(params.get("modelId"));
        if (modelId == null) {
            requirePermission("/api/system/modelData/save@0");
            return DataVo.failure("参数传递错误");
        }
        requirePermission("/api/system/modelData/save@" + modelId);
        // G14 字段级权限：无权字段写入直接拒绝
        DataVo denied = assertFieldsWritable(modelId, params,
                modelFieldService.findFieldsByModelId(modelId, null));
        if (denied != null) {
            return denied;
        }
        params.remove("modelId");
        // G12：版本快照记录操作人
        return modelDataService.insertData(modelId, params, null, getLoginUserId());
    }

    @ResponseBody
    @PostMapping("/system/modelData/update")
    public DataVo dataUpdate(@RequestParam Map<String, String> params) {
        Long modelId = parseLong(params.get("modelId"));
        Long id = parseLong(params.get("id"));
        if (modelId == null || id == null) {
            requirePermission("/api/system/modelData/save@0");
            return DataVo.failure("参数传递错误");
        }
        requirePermission("/api/system/modelData/save@" + modelId);
        // G14 字段级权限：无权字段写入直接拒绝
        DataVo denied = assertFieldsWritable(modelId, params,
                modelFieldService.findFieldsByModelId(modelId, null));
        if (denied != null) {
            return denied;
        }
        params.remove("modelId");
        params.remove("id");
        // G12：版本快照记录操作人
        return modelDataService.updateData(modelId, id, params, getLoginUserId());
    }

    // /////////////////// G20 模型导入导出（schema-as-code） ///////////////////

    @Autowired
    private com.flycms.module.model.service.ModelTransferService modelTransferService;

    /** 导出模型定义（模型+字段+分类的单个 JSON；不含内容数据） */
    @ResponseBody
    @GetMapping("/system/model/export/{id}")
    public DataVo modelExport(@PathVariable Long id) {
        requirePermission("/api/system/model/list");
        Map<String, Object> payload = modelTransferService.exportModel(id);
        if (payload == null) {
            return DataVo.failure("模型不存在");
        }
        return DataVo.success("操作成功", payload);
    }

    /** 导入模型定义（幂等：模型按 code 对齐、字段按 fieldName 对齐、分类按名称对齐） */
    @ResponseBody
    @PostMapping("/system/model/import")
    public DataVo modelImport(@RequestParam(value = "payload", required = false) String payload) {
        requirePermission("/api/system/model/save");
        if (StringUtils.isBlank(payload)) {
            return DataVo.failure("请传入 payload（模型定义 JSON）");
        }
        return modelTransferService.importModel(payload);
    }

    // /////////////////// E10 重新生成骨架（D13 收尾） ///////////////////

    /**
     * 重新生成模型默认模板（list.html / detail.html，强制覆盖当前主题目录下的这两份）。
     * 供模型列表「重新生成骨架」按钮使用；复用模型更新权限，不新增权限节点。
     */
    @ResponseBody
    @PostMapping("/system/model/regenTemplates/{id}")
    public DataVo regenTemplates(@PathVariable Long id) {
        requirePermission("/api/system/model/update");
        return modelService.regenerateDefaultTemplates(id);
    }

    // /////////////////// G15 字段组库（复制式 Component） ///////////////////

    @ResponseBody
    @GetMapping("/system/component/list")
    public DataVo componentList() {
        requirePermission("/api/system/modelField/list");
        return DataVo.success("操作成功", modelTransferService.listComponents());
    }

    @ResponseBody
    @PostMapping("/system/component/save")
    public DataVo componentSave(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelField/save");
        return modelTransferService.saveComponent(params.get("code"), params.get("name"),
                params.get("remark"), params.get("fields"));
    }

    @ResponseBody
    @PostMapping("/system/component/del")
    public DataVo componentDel(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/modelField/save");
        return modelTransferService.deleteComponent(id);
    }

    /** 应用字段组到模型（复制式：新建 group 字段 + 子字段，同名跳过） */
    @ResponseBody
    @PostMapping("/system/component/apply")
    public DataVo componentApply(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                                 @RequestParam(value = "componentId", defaultValue = "0") Long componentId) {
        requirePermission("/api/system/modelField/save");
        return modelTransferService.applyComponent(modelId, componentId);
    }

    /** 把模型里某个 group/repeater 字段（含子字段）另存为字段组 */
    @ResponseBody
    @PostMapping("/system/component/pickup")
    public DataVo componentPickup(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                                  @RequestParam(value = "fieldId", defaultValue = "0") Long fieldId,
                                  @RequestParam(value = "code", required = false) String code,
                                  @RequestParam(value = "name", required = false) String name,
                                  @RequestParam(value = "remark", required = false) String remark) {
        requirePermission("/api/system/modelField/save");
        if (StringUtils.isBlank(code) || StringUtils.isBlank(name)) {
            return DataVo.failure("请填写字段组标识与名称");
        }
        return modelTransferService.pickupComponent(modelId, fieldId, code.trim(), name.trim(), remark);
    }

    /** G16 草稿预览：签发短时效预览 URL（绑定模型+内容，重启失效） */
    @ResponseBody
    @GetMapping("/system/modelData/previewToken")
    public DataVo previewToken(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                               @RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/modelData/save@" + modelId);
        Model model = modelService.findModelById(modelId);
        if (model == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        Map<String, Object> row = modelDataService.findDataById(modelId, id);
        if (row == null) {
            return DataVo.failure("内容不存在");
        }
        String token = com.flycms.core.utils.PreviewTokenUtils.mint(
                getLoginUserId(), model.getCode(), id);
        String url = "/" + model.getCode() + "/" + row.get("short_url") + ".html"
                + "?__preview=1&__pid=" + id + "&__ptoken=" + token;
        return DataVo.success("操作成功", java.util.Map.of("url", url));
    }

    // /////////////////// G12 内容版本 ///////////////////

    /** 版本列表（新→旧，不含快照大字段） */
    @ResponseBody
    @GetMapping("/system/modelData/version/list/{modelId}/{id}")
    public DataVo versionList(@PathVariable Long modelId, @PathVariable Long id,
                              @RequestParam(value = "p", defaultValue = "1") int pageNum,
                              @RequestParam(value = "rows", defaultValue = "10") int rows) {
        requirePermission("/api/system/modelData/save@" + modelId);
        return DataVo.success("操作成功", modelDataService.listVersions(modelId, id, pageNum, rows));
    }

    /** 恢复到指定版本（恢复动作另存为新版本，历史不丢） */
    @ResponseBody
    @PostMapping("/system/modelData/version/restore")
    public DataVo versionRestore(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                                 @RequestParam(value = "id", defaultValue = "0") Long id,
                                 @RequestParam(value = "version", defaultValue = "0") int version) {
        requirePermission("/api/system/modelData/save@" + modelId);
        if (modelId <= 0 || id <= 0 || version <= 0) {
            return DataVo.failure("参数传递错误");
        }
        return modelDataService.restoreVersion(modelId, id, version, getLoginUserId());
    }

    @ResponseBody
    @PostMapping("/system/modelData/del")
    public DataVo dataDel(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                          @RequestParam(value = "ids", required = false) String ids) {
        requirePermission("/api/system/modelData/save@" + modelId);
        if (modelId <= 0 || StringUtils.isBlank(ids)) {
            return DataVo.failure("参数传递错误");
        }
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
        return modelDataService.deleteData(modelId, idList);
    }

    @ResponseBody
    @PostMapping("/system/modelData/status")
    public DataVo dataStatus(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                             @RequestParam(value = "ids", required = false) String ids,
                             @RequestParam(value = "status", defaultValue = "0") int status) {
        requirePermission("/api/system/modelData/save@" + modelId);
        if (modelId <= 0 || StringUtils.isBlank(ids)) {
            return DataVo.failure("参数传递错误");
        }
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
        return modelDataService.updateStatus(modelId, idList, status);
    }

    // /////////////////// 模型分类 ///////////////////

    @ResponseBody
    @GetMapping("/system/modelCategory/tree/{modelId}")
    public DataVo categoryTree(@PathVariable Long modelId) {
        requirePermission("/api/system/modelCategory/tree");
        return DataVo.success("操作成功", modelCategoryService.findCategoriesByModelId(modelId, null));
    }

    @ResponseBody
    @PostMapping("/system/modelCategory/save")
    public DataVo categorySave(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelCategory/save");
        // P9 分类树管理：传 id = 更新（改名/排序/换父级），不传 = 新增
        Long id = parseLong(params.get("id"));
        if (id != null && id > 0) {
            ModelCategory category = modelCategoryService.findCategoryById(id);
            if (category == null) {
                return DataVo.failure("分类不存在");
            }
            if (StringUtils.isNotBlank(params.get("name"))) {
                category.setName(params.get("name"));
            }
            if (params.containsKey("fatherId")) {
                Long fatherId = parseLong(params.get("fatherId"));
                if (fatherId != null && fatherId.equals(id)) {
                    return DataVo.failure("父级不能是自己");
                }
                category.setFatherId(fatherId == null ? 0L : fatherId);
            }
            category.setKeywords(params.get("keywords"));
            category.setDescription(params.get("description"));
            category.setSort(parseInt(params.get("sort"), category.getSort()));
            return modelCategoryService.updateCategory(category);
        }
        ModelCategory category = new ModelCategory();
        category.setModelId(parseLong(params.get("modelId")));
        category.setFatherId(parseLong(params.get("fatherId")));
        category.setName(params.get("name"));
        category.setKeywords(params.get("keywords"));
        category.setDescription(params.get("description"));
        category.setSort(parseInt(params.get("sort"), 0));
        if (category.getModelId() == null || StringUtils.isBlank(category.getName())) {
            return DataVo.failure("参数传递错误");
        }
        return modelCategoryService.addCategory(category);
    }

    @ResponseBody
    @PostMapping("/system/modelCategory/del")
    public DataVo categoryDel(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/modelCategory/save");
        return modelCategoryService.deleteCategory(id);
    }

    // /////////////////// 附件列表（AttachmentPicker 数据源，fly_images） ///////////////////

    @ResponseBody
    @GetMapping("/system/attachment/list")
    public DataVo attachmentList(@RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/api/system/modelData/list");
        PageVo<Map<String, Object>> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(20);
        pageVo.setList(modelDataDao.attachmentList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(modelDataDao.attachmentCount());
        return DataVo.success("操作成功", pageVo);
    }

    // /////////////////// 工具 ///////////////////

    /** 空串转 null（P1/P2 新字段：显式传空 = 清除配置） */
    private String blankToNull(String v) {
        return StringUtils.isBlank(v) ? null : v;
    }

    private Long parseLong(String v) {
        try {
            return v == null ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String v) {
        try {
            return v == null ? null : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String v, int def) {
        Integer r = parseInteger(v);
        return r == null ? def : r;
    }

    private java.math.BigDecimal parseDecimal(String v) {
        try {
            return v == null || v.trim().isEmpty() ? null : new java.math.BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
