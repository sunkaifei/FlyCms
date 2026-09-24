package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.form.model.Form;
import com.flycms.module.form.model.FormData;
import com.flycms.module.form.model.FormField;
import com.flycms.module.form.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 表单生成器后台 REST（规划阶段 F）
 *
 * 覆盖：表单 CRUD、字段设计器 CRUD、数据查看/审核/删除/CSV 导出。
 * 前台公开提交端点在 ApiFormSubmitController（不走管理员鉴权）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiFormController extends ApiBaseController {

    @Autowired
    private FormService formService;

    // ///////////// 表单 /////////////

    @ResponseBody
    @GetMapping("/system/form/page")
    public DataVo page(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows,
                       @RequestParam(value = "formName", required = false) String formName,
                       @RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/form/page");
        PageVo<Form> pageVo = formService.getFormPage(formName, status, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/form/get")
    public DataVo get(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/form/page");
        Form form = formService.findFormById(id);
        if (form == null) {
            return DataVo.failure("表单不存在");
        }
        return DataVo.success("操作成功", form);
    }

    @ResponseBody
    @PostMapping("/system/form/save")
    public DataVo save(@RequestParam java.util.Map<String, String> params) {
        requirePermission("/api/system/form/save");
        Form form = new Form();
        String id = params.get("id");
        if (id != null && !id.trim().isEmpty()) {
            try {
                form.setId(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                return DataVo.failure("id 不合法");
            }
        }
        form.setFormCode(params.get("formCode"));
        form.setFormName(params.get("formName"));
        form.setAudit(parseInt(params.get("audit"), 0));
        form.setSubmitLimit(parseInt(params.get("submitLimit"), 1));
        form.setNeedCaptcha(parseInt(params.get("needCaptcha"), 1));
        form.setSuccessTip(params.get("successTip"));
        form.setNotifyEmail(params.get("notifyEmail"));
        form.setStatus(parseInt(params.get("status"), 1));
        return formService.saveForm(form);
    }

    @ResponseBody
    @PostMapping("/system/form/delete")
    public DataVo delete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/form/delete");
        return formService.deleteForm(id);
    }

    // ///////////// 表单字段 /////////////

    @ResponseBody
    @GetMapping("/system/formField/list")
    public DataVo fieldList(@RequestParam(value = "formId", defaultValue = "0") Long formId) {
        requirePermission("/api/system/form/page");
        List<FormField> list = formService.getFieldList(formId);
        return DataVo.success("操作成功", list);
    }

    @ResponseBody
    @PostMapping("/system/formField/save")
    public DataVo fieldSave(@RequestParam java.util.Map<String, String> params) {
        requirePermission("/api/system/form/save");
        FormField field = new FormField();
        String id = params.get("id");
        if (id != null && !id.trim().isEmpty()) {
            try {
                field.setId(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                return DataVo.failure("id 不合法");
            }
        }
        String formId = params.get("formId");
        if (formId == null || formId.trim().isEmpty()) {
            return DataVo.failure("缺少 formId");
        }
        try {
            field.setFormId(Long.parseLong(formId.trim()));
        } catch (NumberFormatException e) {
            return DataVo.failure("formId 不合法");
        }
        field.setFieldCode(params.get("fieldCode"));
        field.setFieldName(params.get("fieldName"));
        field.setFieldType(params.get("fieldType"));
        field.setRequired(parseInt(params.get("required"), 0));
        field.setDefaultValue(params.get("defaultValue"));
        field.setPlaceholder(params.get("placeholder"));
        field.setOptions(params.get("options"));
        field.setSort(parseInt(params.get("sort"), 0));
        return formService.saveField(field);
    }

    @ResponseBody
    @PostMapping("/system/formField/delete")
    public DataVo fieldDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/form/delete");
        return formService.deleteField(id);
    }

    // ///////////// 表单数据 /////////////

    @ResponseBody
    @GetMapping("/system/formData/page")
    public DataVo dataPage(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                           @RequestParam(value = "rows", defaultValue = "20") int rows,
                           @RequestParam(value = "formId", required = false) Long formId,
                           @RequestParam(value = "status", required = false) Integer status,
                           @RequestParam(value = "createTime", required = false) String createTime) {
        requirePermission("/api/system/formData/page");
        PageVo<FormData> pageVo = formService.getDataPage(formId, status, createTime, pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    @ResponseBody
    @GetMapping("/system/formData/detail")
    public DataVo dataDetail(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/formData/page");
        FormData data = formService.findDataById(id);
        if (data == null) {
            return DataVo.failure("数据不存在");
        }
        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("id", data.getId());
        result.put("formId", data.getFormId());
        result.put("formName", data.getFormName());
        result.put("userId", data.getUserId());
        result.put("ip", data.getIp());
        result.put("status", data.getStatus());
        result.put("createTime", data.getCreateTime());
        result.put("values", formService.parseDataJson(data.getDataJson()));
        return DataVo.success("操作成功", result);
    }

    /** 数据审核：status 1通过 2不通过 */
    @ResponseBody
    @PostMapping("/system/formData/audit")
    public DataVo dataAudit(@RequestParam(value = "id", defaultValue = "0") Long id,
                            @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/formData/audit");
        return formService.auditData(id, status);
    }

    @ResponseBody
    @PostMapping("/system/formData/delete")
    public DataVo dataDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/formData/delete");
        return formService.deleteData(id);
    }

    /**
     * CSV 导出。带 UTF-8 BOM 头，Excel 打开不乱码。
     */
    @GetMapping("/system/formData/export")
    public void export(@RequestParam(value = "formId", required = false) Long formId,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "createTime", required = false) String createTime,
                       jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        requirePermission("/api/system/formData/page");
        String csv = formService.exportCsv(formId, status, createTime);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        String fileName = "form-" + (formId == null ? "all" : formId) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(bom);
        response.getOutputStream().write(csv.getBytes("UTF-8"));
        response.getOutputStream().flush();
    }

    private int parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
