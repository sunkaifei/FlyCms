package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.dict.model.DictData;
import com.flycms.module.dict.model.DictType;
import com.flycms.module.dict.service.DictService;
import com.flycms.module.model.dao.ModelFieldDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典 REST 接口（vben 后台，fly_dict_type / fly_dict_data）。
 *
 * 权限码 = action_key 原样（/api/system/dict/**，见 sql/migrations/2026-09-30-dict.sql）；
 * 未登录由 requireAdmin 抛 HTTP 401，无权限 requirePermission 抛 403。
 * 读侧两个端点（type/options、data/type/*）只要求登录：发布表单渲染候选项时
 * 内容编辑未必持有字典管理权限，与 user/category 候选项端点同口径。
 * 前端表单 POST 统一 form-urlencoded（@RequestParam 读取）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/dict")
public class ApiDictController extends ApiBaseController {

    @Autowired
    private DictService dictService;
    @Autowired
    private ModelFieldDao modelFieldDao;

    // /////////////////// 字典类型 ///////////////////

    /** 类型分页列表（keyword 模糊匹配名称/类型键） */
    @ResponseBody
    @GetMapping("/type/list")
    public DataVo typeList(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                           @RequestParam(value = "rows", defaultValue = "20") int rows,
                           @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/dict/type/list");
        PageVo<DictType> pageVo = dictService.findTypePage(pageNum, rows, keyword);
        return DataVo.success("操作成功", pageVo);
    }

    /** 全部启用类型（字段设置「绑定字典」下拉用，只要求登录） */
    @ResponseBody
    @GetMapping("/type/options")
    public DataVo typeOptions() {
        requireAdmin();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DictType t : dictService.findEnabledTypes()) {
            Map<String, Object> row = new HashMap<>();
            row.put("dictName", t.getDictName());
            row.put("dictType", t.getDictType());
            list.add(row);
        }
        return DataVo.success("操作成功", list);
    }

    /** 新增类型（dictName/dictType/remark/sort/status） */
    @ResponseBody
    @PostMapping("/type/save")
    public DataVo typeSave(@RequestParam(value = "dictName", required = false) String dictName,
                           @RequestParam(value = "dictType", required = false) String dictType,
                           @RequestParam(value = "remark", required = false) String remark,
                           @RequestParam(value = "sort", defaultValue = "0") int sort,
                           @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/dict/type/save");
        DictType form = new DictType();
        form.setDictName(dictName);
        form.setDictType(dictType);
        form.setRemark(remark);
        form.setSort(sort);
        form.setStatus(status);
        return dictService.addType(form);
    }

    /** 编辑类型（id 必传；dictType 键创建后不可改） */
    @ResponseBody
    @PostMapping("/type/update")
    public DataVo typeUpdate(@RequestParam(value = "id", required = false) Long id,
                             @RequestParam(value = "dictName", required = false) String dictName,
                             @RequestParam(value = "remark", required = false) String remark,
                             @RequestParam(value = "sort", defaultValue = "0") int sort,
                             @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/dict/type/update");
        DictType form = new DictType();
        form.setId(id);
        form.setDictName(dictName);
        form.setRemark(remark);
        form.setSort(sort);
        form.setStatus(status);
        return dictService.updateType(form);
    }

    /** 删除类型（级联删数据；被模型字段引用时拒绝） */
    @ResponseBody
    @PostMapping("/type/delete")
    public DataVo typeDelete(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/dict/type/delete");
        if (id == null || id <= 0) {
            return DataVo.failure("字典类型 id 不能为空");
        }
        DictType type = dictService.findTypeById(id);
        if (type == null) {
            return DataVo.failure("字典类型不存在");
        }
        int bindCount = modelFieldDao.countFieldsByDictType(type.getDictType());
        return dictService.deleteType(id, bindCount);
    }

    // /////////////////// 字典数据 ///////////////////

    /** 数据分页列表（dictType 必传，keyword 模糊匹配标签/键值） */
    @ResponseBody
    @GetMapping("/data/list")
    public DataVo dataList(@RequestParam(value = "dictType", required = false) String dictType,
                           @RequestParam(value = "p", defaultValue = "1") int pageNum,
                           @RequestParam(value = "rows", defaultValue = "20") int rows,
                           @RequestParam(value = "keyword", required = false) String keyword) {
        requirePermission("/api/system/dict/data/list");
        if (dictType == null || dictType.isBlank()) {
            return DataVo.failure("dictType 不能为空");
        }
        PageVo<DictData> pageVo = dictService.findDataPage(pageNum, rows, dictType, keyword);
        return DataVo.success("操作成功", pageVo);
    }

    /** 某字典类型的启用数据（发布/筛选表单候选项，只要求登录） */
    @ResponseBody
    @GetMapping("/data/type/{dictType}")
    public DataVo dataByType(@org.springframework.web.bind.annotation.PathVariable("dictType") String dictType) {
        requireAdmin();
        return DataVo.success("操作成功", dictService.findEnabledData(dictType));
    }

    /** 新增数据（dictType/dictLabel/dictValue/remark/sort/status） */
    @ResponseBody
    @PostMapping("/data/save")
    public DataVo dataSave(@RequestParam(value = "dictType", required = false) String dictType,
                           @RequestParam(value = "dictLabel", required = false) String dictLabel,
                           @RequestParam(value = "dictValue", required = false) String dictValue,
                           @RequestParam(value = "remark", required = false) String remark,
                           @RequestParam(value = "sort", defaultValue = "0") int sort,
                           @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/dict/data/save");
        DictData form = new DictData();
        form.setDictType(dictType);
        form.setDictLabel(dictLabel);
        form.setDictValue(dictValue);
        form.setRemark(remark);
        form.setSort(sort);
        form.setStatus(status);
        return dictService.addData(form);
    }

    /** 编辑数据（id 必传；所属类型不可改） */
    @ResponseBody
    @PostMapping("/data/update")
    public DataVo dataUpdate(@RequestParam(value = "id", required = false) Long id,
                             @RequestParam(value = "dictLabel", required = false) String dictLabel,
                             @RequestParam(value = "dictValue", required = false) String dictValue,
                             @RequestParam(value = "remark", required = false) String remark,
                             @RequestParam(value = "sort", defaultValue = "0") int sort,
                             @RequestParam(value = "status", defaultValue = "1") int status) {
        requirePermission("/api/system/dict/data/update");
        DictData form = new DictData();
        form.setId(id);
        form.setDictLabel(dictLabel);
        form.setDictValue(dictValue);
        form.setRemark(remark);
        form.setSort(sort);
        form.setStatus(status);
        return dictService.updateData(form);
    }

    /** 删除数据 */
    @ResponseBody
    @PostMapping("/data/delete")
    public DataVo dataDelete(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/dict/data/delete");
        if (id == null || id <= 0) {
            return DataVo.failure("字典数据 id 不能为空");
        }
        return dictService.deleteData(id);
    }
}
