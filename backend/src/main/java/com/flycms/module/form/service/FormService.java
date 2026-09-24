package com.flycms.module.form.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.IpUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.form.dao.FormDao;
import com.flycms.module.form.model.Form;
import com.flycms.module.form.model.FormData;
import com.flycms.module.form.model.FormField;
import com.flycms.module.other.service.EmailService;
import com.flycms.module.other.service.FilterKeywordService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 表单系统服务（规划阶段 F）
 *
 * 存储策略：表单定义与字段走 fly_form / fly_form_field；
 * 提交数据统一放 fly_form_data + data_json（不动态建表，理由见 Form 类注释）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class FormService {

    private static final Logger log = LoggerFactory.getLogger(FormService.class);

    @Autowired
    private FormDao formDao;
    @Autowired
    private FilterKeywordService filterKeywordService;
    @Autowired
    private EmailService emailService;

    // ///////////////////////////////
    // /////       表单 CRUD      /////
    // ///////////////////////////////

    public PageVo<Form> getFormPage(String formName, Integer status, int pageNum, int rows) {
        PageVo<Form> pageVo = new PageVo<Form>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(formDao.getFormList(formName, status, pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(formDao.getFormCount(formName, status));
        return pageVo;
    }

    public Form findFormById(Long id) {
        return id == null ? null : formDao.findFormById(id);
    }

    public Form findFormByCode(String formCode) {
        return StringUtils.isBlank(formCode) ? null : formDao.findFormByCode(formCode.trim());
    }

    @Transactional
    public DataVo saveForm(Form form) {
        if (form == null || StringUtils.isBlank(form.getFormCode()) || StringUtils.isBlank(form.getFormName())) {
            return DataVo.failure("表单编码与名称不能为空");
        }
        form.setFormCode(form.getFormCode().trim());
        // 编码只允许字母数字与下划线：它直接出现在公开 URL 中
        if (!form.getFormCode().matches("[A-Za-z0-9_]+")) {
            return DataVo.failure("表单编码只能包含字母、数字和下划线");
        }
        if (formDao.checkFormCode(form.getFormCode(), form.getId()) > 0) {
            return DataVo.failure("表单编码已存在");
        }
        if (form.getAudit() == null) {
            form.setAudit(0);
        }
        if (form.getSubmitLimit() == null) {
            form.setSubmitLimit(1);
        }
        if (form.getNeedCaptcha() == null) {
            form.setNeedCaptcha(1);
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        if (form.getId() == null || formDao.findFormById(form.getId()) == null) {
            form.setId(SnowFlake.getInstance().nextId());
            form.setCreateTime(new Date());
            formDao.addForm(form);
            return DataVo.success("新增成功");
        }
        formDao.updateForm(form);
        return DataVo.success("操作成功");
    }

    @Transactional
    public DataVo deleteForm(Long id) {
        if (id == null) {
            return DataVo.failure("参数不完整");
        }
        // 表单定义删除时一并清理字段（数据保留，避免误删用户提交记录）
        formDao.deleteFormFieldByFormId(id);
        int rows = formDao.deleteForm(id);
        return rows > 0 ? DataVo.success("删除成功") : DataVo.failure("表单不存在");
    }

    // ///////////////////////////////
    // /////       表单字段      /////
    // ///////////////////////////////

    public List<FormField> getFieldList(Long formId) {
        if (formId == null) {
            return new ArrayList<FormField>();
        }
        return formDao.getFormFieldList(formId);
    }

    public FormField findFieldById(Long id) {
        return id == null ? null : formDao.findFormFieldById(id);
    }

    @Transactional
    public DataVo saveField(FormField field) {
        if (field == null || field.getFormId() == null) {
            return DataVo.failure("参数不完整");
        }
        if (StringUtils.isBlank(field.getFieldCode()) || StringUtils.isBlank(field.getFieldName())) {
            return DataVo.failure("字段标识与名称不能为空");
        }
        field.setFieldCode(field.getFieldCode().trim());
        if (!field.getFieldCode().matches("[A-Za-z0-9_]+")) {
            return DataVo.failure("字段标识只能包含字母、数字和下划线");
        }
        if (StringUtils.isBlank(field.getFieldType())) {
            field.setFieldType("input");
        }
        if (field.getRequired() == null) {
            field.setRequired(0);
        }
        if (field.getSort() == null) {
            field.setSort(0);
        }
        if (field.getId() == null || formDao.findFormFieldById(field.getId()) == null) {
            field.setId(SnowFlake.getInstance().nextId());
            formDao.addFormField(field);
            return DataVo.success("新增成功");
        }
        formDao.updateFormField(field);
        return DataVo.success("操作成功");
    }

    @Transactional
    public DataVo deleteField(Long id) {
        if (id == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = formDao.deleteFormField(id);
        return rows > 0 ? DataVo.success("删除成功") : DataVo.failure("字段不存在");
    }

    // ///////////////////////////////
    // /////       表单数据      /////
    // ///////////////////////////////

    public PageVo<FormData> getDataPage(Long formId, Integer status, String createTime, int pageNum, int rows) {
        PageVo<FormData> pageVo = new PageVo<FormData>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(formDao.getFormDataList(formId, status, createTime, pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(formDao.getFormDataCount(formId, status, createTime));
        return pageVo;
    }

    public FormData findDataById(Long id) {
        return id == null ? null : formDao.findFormDataById(id);
    }

    @Transactional
    public DataVo deleteData(Long id) {
        if (id == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = formDao.deleteFormData(id);
        return rows > 0 ? DataVo.success("删除成功") : DataVo.failure("数据不存在");
    }

    /** 表单数据审核：1通过 2不通过 */
    @Transactional
    public DataVo auditData(Long id, Integer status) {
        if (id == null || status == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = formDao.updateFormDataStatus(id, status);
        return rows > 0 ? DataVo.success("操作成功") : DataVo.failure("数据不存在");
    }

    /**
     * 导出 CSV（带 BOM 头，防止 Excel 打开中文乱码）
     */
    public String exportCsv(Long formId, Integer status, String createTime) {
        List<FormField> fields = getFieldList(formId);
        List<FormData> list = formDao.getFormDataList(formId, status, createTime, 0, 5000);
        StringBuilder sb = new StringBuilder();
        // 表头
        sb.append("提交时间,状态");
        for (FormField f : fields) {
            sb.append(',').append(escapeCsv(f.getFieldName()));
        }
        sb.append('\n');
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (FormData d : list) {
            sb.append(d.getCreateTime() == null ? "" : sdf.format(d.getCreateTime()));
            sb.append(',').append(statusText(d.getStatus()));
            Map<String, String> values = parseDataJson(d.getDataJson());
            for (FormField f : fields) {
                sb.append(',').append(escapeCsv(values.get(f.getFieldCode())));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0: return "待审";
            case 1: return "正常";
            case 2: return "不通过";
            default: return String.valueOf(status);
        }
    }

    private String escapeCsv(String v) {
        if (v == null) {
            return "";
        }
        String s = v.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    /**
     * 极简 JSON 解析：表单 data_json 由本服务写出，结构是一层 key-value，
     * 这里手动解析避免引入额外依赖与复杂转义分支。
     */
    public Map<String, String> parseDataJson(String json) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (StringUtils.isBlank(json)) {
            return map;
        }
        String s = json.trim();
        if (s.startsWith("{")) {
            s = s.substring(1, s.length() - 1);
        }
        // 按逗号切分（值内不含逗号，因存储时已做转义替换）
        String[] parts = s.split(",");
        for (String part : parts) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String k = part.substring(0, idx).trim().replace("\"", "");
            String v = part.substring(idx + 1).trim().replace("\"", "");
            v = v.replace("\\n", "\n").replace("\\,", ",");
            map.put(k, v);
        }
        return map;
    }

    // ///////////////////////////////
    // /////     前台公开提交    /////
    // ///////////////////////////////

    /**
     * 前台提交表单。
     *
     * 防护：表单启用校验 → 频控（同一 IP/用户每日限次）→ 必填校验 → jsoup 清洗 → 敏感词过滤。
     * audit=0 时直接落 status=1；audit=1 时落 status=0 待后台审核。
     *
     * @param values    字段值（key=fieldCode）
     * @param captcha   验证码，need_captcha=1 时必填
     * @param sessionCaptcha 会话中的验证码，由 Controller 取出后传入（Service 不直接依赖 Session）
     */
    @Transactional
    public DataVo submit(HttpServletRequest request, String formCode, Map<String, String> values,
                         String captcha, String sessionCaptcha, Long userId) {
        Form form = findFormByCode(formCode);
        if (form == null || form.getStatus() == null || form.getStatus() != 1) {
            return DataVo.failure("表单不存在或已停用");
        }
        // 验证码
        if (form.getNeedCaptcha() != null && form.getNeedCaptcha() == 1) {
            if (StringUtils.isBlank(captcha) || sessionCaptcha == null
                    || !captcha.equalsIgnoreCase(sessionCaptcha)) {
                return DataVo.failure("验证码不正确");
            }
        }
        // 频控：同一 IP / 同一用户每日限次（以当天零点为界）
        String ip = IpUtils.getIpAddr(request);
        String since = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 00:00:00";
        int limit = form.getSubmitLimit() == null ? 1 : form.getSubmitLimit();
        if (limit > 0) {
            if (StringUtils.isNotBlank(ip) && formDao.countSubmitByIp(form.getId(), ip, since) >= limit) {
                return DataVo.failure("今日提交次数已达上限");
            }
            if (userId != null && userId > 0
                    && formDao.countSubmitByUser(form.getId(), userId, since) >= limit) {
                return DataVo.failure("今日提交次数已达上限");
            }
        }

        List<FormField> fields = getFieldList(form.getId());
        if (fields.isEmpty()) {
            return DataVo.failure("表单未配置字段");
        }
        // 校验 + 清洗
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (FormField f : fields) {
            String raw = values == null ? null : values.get(f.getFieldCode());
            if (raw == null || raw.trim().isEmpty()) {
                raw = f.getDefaultValue();
            }
            if ((raw == null || raw.trim().isEmpty()) && f.getRequired() != null && f.getRequired() == 1) {
                return DataVo.failure("请填写" + f.getFieldName());
            }
            String val = raw == null ? "" : raw.trim();
            // XSS 清洗：统一去标签，只保留纯文本
            val = Jsoup.clean(val, Safelist.none());
            // 敏感词过滤
            if (filterKeywordService != null) {
                for (String word : safeKeywordList()) {
                    if (StringUtils.isNotBlank(word) && val.contains(word)) {
                        return DataVo.failure("提交内容包含敏感词，请修改后重试");
                    }
                }
            }
            if (!first) {
                json.append(',');
            }
            json.append('"').append(f.getFieldCode()).append("\":\"")
                    .append(val.replace("\\", "").replace("\"", "").replace("\n", "\\n").replace(",", "\\,"))
                    .append('"');
            first = false;
        }
        json.append("}");

        FormData data = new FormData();
        data.setId(SnowFlake.getInstance().nextId());
        data.setFormId(form.getId());
        data.setUserId(userId == null ? 0L : userId);
        data.setIp(ip);
        data.setDataJson(json.toString());
        // audit=1 先审后发（0待审）；audit=0 直接通过
        data.setStatus(form.getAudit() != null && form.getAudit() == 1 ? 0 : 1);
        data.setCreateTime(new Date());
        formDao.addFormData(data);

        // 邮件通知（异步，失败不影响提交结果）
        notifyByEmail(form, json.toString());

        String tip = StringUtils.isBlank(form.getSuccessTip()) ? "提交成功" : form.getSuccessTip();
        return DataVo.success(tip);
    }

    private List<String> safeKeywordList() {
        try {
            List<String> list = filterKeywordService.getFilterKeywordAllList();
            return list == null ? Collections.<String>emptyList() : list;
        } catch (Exception e) {
            log.warn("获取敏感词列表失败，跳过敏感词校验", e);
            return Collections.emptyList();
        }
    }

    /**
     * 表单提交通知。
     * 这里同步调用但内部吞掉异常；若邮件服务器慢，可改为 @Async 或线程池。
     */
    private void notifyByEmail(final Form form, final String dataJson) {
        if (form == null || StringUtils.isBlank(form.getNotifyEmail()) || emailService == null) {
            return;
        }
        try {
            Map<String, String> map = parseDataJson(dataJson);
            StringBuilder body = new StringBuilder();
            body.append("<p>表单【").append(form.getFormName()).append("】收到新的提交：</p><ul>");
            for (Map.Entry<String, String> e : map.entrySet()) {
                body.append("<li>").append(e.getKey()).append("：").append(e.getValue()).append("</li>");
            }
            body.append("</ul>");
            emailService.sendNotifyEmail(form.getNotifyEmail(),
                    "表单【" + form.getFormName() + "】新提交通知", body.toString());
        } catch (Exception e) {
            log.error("表单提交通知邮件发送失败, formCode={}", form.getFormCode(), e);
        }
    }
}
