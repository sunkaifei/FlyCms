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
        return modelService.addModel(model);
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
        return modelService.updateModel(model);
    }

    @ResponseBody
    @PostMapping("/system/model/del")
    public DataVo modelDel(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/model/del");
        return modelService.deleteModel(id);
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
        field.setRegex(params.get("regex"));
        field.setPlaceholder(params.get("placeholder"));
        field.setTips(params.get("tips"));
        field.setRelateModel(params.get("relateModel"));
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
        field.setRegex(params.get("regex"));
        field.setPlaceholder(params.get("placeholder"));
        field.setTips(params.get("tips"));
        field.setRelateModel(params.get("relateModel"));
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
        requirePermission("/api/system/modelData/list");
        Map<String, String> filters = new HashMap<>(allParams);
        filters.remove("p");
        filters.remove("rows");
        filters.remove("title");
        filters.remove("status");
        filters.remove("categoryId");
        filters.remove("orderby");
        filters.remove("order");
        // rows 由调用方指定（关联选择器需一次取更多候选），上限 200 防滥用
        int pageSize = Math.min(Math.max(rows, 1), 200);
        PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
                modelId, title, categoryId, status, filters, orderby, order, pageNum, pageSize, null, false);
        // 附件/关联展开（E1）：后台列表也能显示关联内容名而非裸 id
        modelDataService.expandAttachments(modelId, pageVo.getList());
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/modelData/formMeta/{modelId}")
    public DataVo formMeta(@PathVariable Long modelId) {
        requirePermission("/api/system/modelData/list");
        return DataVo.success("操作成功", modelDataService.formMeta(modelId));
    }

    @ResponseBody
    @GetMapping("/system/modelData/detail/{modelId}/{id}")
    public DataVo dataDetail(@PathVariable Long modelId, @PathVariable Long id) {
        requirePermission("/api/system/modelData/list");
        Map<String, Object> row = modelDataService.findDataById(modelId, id);
        if (row == null) {
            return DataVo.failure("内容不存在");
        }
        // 附件/关联展开（E1）：编辑弹窗需要 {field}Obj / {field}List 做回显
        modelDataService.expandAttachments(modelId, java.util.Collections.singletonList(row));
        return DataVo.success("操作成功", row);
    }

    @ResponseBody
    @PostMapping("/system/modelData/save")
    public DataVo dataSave(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelData/save");
        Long modelId = parseLong(params.get("modelId"));
        if (modelId == null) {
            return DataVo.failure("参数传递错误");
        }
        params.remove("modelId");
        return modelDataService.insertData(modelId, params, null);
    }

    @ResponseBody
    @PostMapping("/system/modelData/update")
    public DataVo dataUpdate(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/modelData/save");
        Long modelId = parseLong(params.get("modelId"));
        Long id = parseLong(params.get("id"));
        if (modelId == null || id == null) {
            return DataVo.failure("参数传递错误");
        }
        params.remove("modelId");
        params.remove("id");
        return modelDataService.updateData(modelId, id, params);
    }

    @ResponseBody
    @PostMapping("/system/modelData/del")
    public DataVo dataDel(@RequestParam(value = "modelId", defaultValue = "0") Long modelId,
                          @RequestParam(value = "ids", required = false) String ids) {
        requirePermission("/api/system/modelData/save");
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
        requirePermission("/api/system/modelData/save");
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
}
