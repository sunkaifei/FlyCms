package com.flycms.module.model.service;

import com.alibaba.fastjson.JSON;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ShortUrlUtils;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.model.dao.ModelDataDao;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.enums.FieldTypeEnum;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.model.ModelField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义模型动态内容 CRUD：元数据驱动的校验、动态列拼装、fly_images 引用计数。
 * 动态 SQL 安全边界：whereSql/orderBySql 仅由本服务依元数据白名单生成，参数全部 #{params.*} 预编译。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelDataService {

    @Autowired
    private ModelDataDao modelDataDao;
    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private ModelTableService modelTableService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelCategoryService modelCategoryService;

    // /////////////////// 查询 ///////////////////

    /**
     * 后台/前台通用的动态列表查询。
     *
     * @param front   true 时强制 status=1（前台）
     * @param filters 自定义字段筛选（仅 is_filter=1 字段生效），key=字段名
     */
    public PageVo<Map<String, Object>> selectPage(Long modelId, String title, Long categoryId, Integer status,
                                                  Map<String, String> filters, String orderby, String order,
                                                  int page, int rows, Long notId, boolean front) {
        Model model = modelService.findModelById(modelId);
        PageVo<Map<String, Object>> pageVo = new PageVo<>(page);
        pageVo.setRows(rows);
        if (model == null) {
            pageVo.setList(new ArrayList<>());
            return pageVo;
        }
        modelTableService.ensureTable(model.getCode(), modelFieldDao.findFieldsByModelId(modelId, 1));

        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
        Map<String, ModelField> fieldMap = new HashMap<>();
        for (ModelField f : fields) {
            fieldMap.put(f.getFieldName(), f);
        }

        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (front) {
            where.add("status = 1");
        } else if (status != null) {
            where.add("status = #{params.status}");
            params.put("status", status);
        }
        if (StringUtils.isNotBlank(title)) {
            where.add("title LIKE CONCAT('%', #{params.title}, '%')");
            params.put("title", title);
        }
        if (categoryId != null && categoryId > 0) {
            where.add("category_id = #{params.categoryId}");
            params.put("categoryId", categoryId);
        }
        if (notId != null && notId > 0) {
            where.add("id != #{params.notId}");
            params.put("notId", notId);
        }
        // 自定义字段筛选：仅 is_filter=1 且值非空
        for (ModelField f : fields) {
            if (f.getIsFilter() != 1) {
                continue;
            }
            String v = filters == null ? null : filters.get(f.getFieldName());
            if (StringUtils.isBlank(v)) {
                continue;
            }
            where.add("`" + SqlSafeUtil.safeColumnName(f.getFieldName()) + "` = #{params." + f.getFieldName() + "}");
            params.put(f.getFieldName(), v);
        }

        // 排序白名单：固有列或元数据字段，方向限定 asc/desc
        String col = "id";
        try {
            col = SqlSafeUtil.safeOrderColumn(StringUtils.defaultIfBlank(orderby, "id"));
        } catch (IllegalArgumentException ignored) {
        }
        String dir = "asc".equalsIgnoreCase(order) ? "asc" : "desc";
        String orderBySql = "`" + col + "` " + dir;

        params.put("offset", pageVo.getOffset());
        params.put("rows", pageVo.getRows());
        String whereSql = String.join(" AND ", where);
        pageVo.setList(modelDataDao.selectPage(tableSuffixOf(modelId), whereSql, orderBySql, params));
        pageVo.setCount(modelDataDao.countPage(tableSuffixOf(modelId), whereSql, params));
        return pageVo;
    }

    public Map<String, Object> findDataById(Long modelId, Long id) {
        return modelDataDao.findDataById(tableSuffixOf(modelId), id);
    }

    /** 前台详情：shortUrl 精确查询，强制 status=1 */
    public Map<String, Object> findByShortUrl(Long modelId, String shortUrl) {
        return modelDataDao.findByShortUrl(tableSuffixOf(modelId), shortUrl);
    }

    /**
     * 动态表单元数据：模型 + 启用字段 + 分类（vben schema 据此渲染）
     */
    public Map<String, Object> formMeta(Long modelId) {
        Model model = modelService.findModelById(modelId);
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
        List<ModelCategory> categories = modelCategoryService.findCategoriesByModelId(modelId, 1);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("model", model);
        meta.put("fields", fields);
        meta.put("categories", categories);
        return meta;
    }

    // /////////////////// 写入 ///////////////////

    public DataVo insertData(Long modelId, Map<String, String> form, Long userId) {
        Model model = modelService.findModelById(modelId);
        if (model == null || model.getStatus() != 1) {
            return DataVo.failure("模型不存在或已禁用");
        }
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);

        // title 必填（固有列）
        if (StringUtils.isBlank(form.get("title"))) {
            return DataVo.failure(model.getTitleLabel() + "不能为空");
        }

        Map<String, Object> values = new HashMap<>();
        List<String> columns = new ArrayList<>();
        List<Long> refIds = new ArrayList<>();
        DataVo check = buildDynamicValues(fields, form, values, columns, refIds);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }

        SnowFlake snowFlake = SnowFlake.getInstance();
        long id = snowFlake.nextId();
        values.put("id", id);
        values.put("shortUrl", genShortUrl(modelId));
        if (userId != null) {
            columns.add("user_id");
            values.put("user_id", userId);
        }
        putColumn(columns, values, "title", form.get("title"));
        putColumn(columns, values, "keywords", form.get("keywords"));
        putColumn(columns, values, "description", form.get("description"));
        putColumn(columns, values, "content", form.get("content"));
        putColumn(columns, values, "category_id", form.get("categoryId"));
        putColumn(columns, values, "thumbnail", form.get("thumbnail"));
        if (form.get("thumbnail") != null && StringUtils.isNotBlank(form.get("thumbnail"))) {
            refIds.add(Long.parseLong(form.get("thumbnail")));
        }
        columns.add("status");
        values.put("status", StringUtils.defaultIfBlank(form.get("status"), "0"));
        if (StringUtils.isNotBlank(form.get("publish_time"))) {
            columns.add("publish_time");
            values.put("publish_time", form.get("publish_time"));
        }

        modelDataDao.insertData(tableSuffixOf(modelId), columns, values);
        if (!refIds.isEmpty()) {
            modelDataDao.incrImagesRefCount(distinct(refIds));
        }
        return DataVo.success("发布成功", id);
    }

    public DataVo updateData(Long modelId, Long id, Map<String, String> form) {
        Map<String, Object> old = modelDataDao.findDataById(tableSuffixOf(modelId), id);
        if (old == null) {
            return DataVo.failure("内容不存在");
        }
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
        if (StringUtils.isBlank(form.get("title"))) {
            return DataVo.failure("标题不能为空");
        }
        Map<String, Object> values = new HashMap<>();
        List<String> columns = new ArrayList<>();
        List<Long> oldRefs = collectAttachmentIds(fields, old);
        List<Long> newRefs = new ArrayList<>();
        DataVo check = buildDynamicValues(fields, form, values, columns, newRefs);
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        values.put("id", id);
        putColumn(columns, values, "title", form.get("title"));
        putColumn(columns, values, "keywords", form.get("keywords"));
        putColumn(columns, values, "description", form.get("description"));
        putColumn(columns, values, "content", form.get("content"));
        putColumn(columns, values, "category_id", form.get("categoryId"));
        putColumn(columns, values, "thumbnail", form.get("thumbnail"));
        if (form.get("thumbnail") != null && StringUtils.isNotBlank(form.get("thumbnail"))) {
            newRefs.add(Long.parseLong(form.get("thumbnail")));
        }
        if (StringUtils.isNotBlank(form.get("publish_time"))) {
            putColumn(columns, values, "publish_time", form.get("publish_time"));
        }

        modelDataDao.updateData(tableSuffixOf(modelId), id, columns, values);
        adjustRefCounts(oldRefs, distinct(newRefs));
        return DataVo.success("更新成功");
    }

    /**
     * 软删（status=3）+ 引用计数 -1
     */
    public DataVo deleteData(Long modelId, List<Long> ids) {
        for (Long id : ids) {
            Map<String, Object> row = modelDataDao.findDataById(tableSuffixOf(modelId), id);
            if (row != null) {
                List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
                List<Long> refs = collectAttachmentIds(fields, row);
                if (!refs.isEmpty()) {
                    modelDataDao.decrImagesRefCount(distinct(refs));
                }
            }
        }
        modelDataDao.deleteData(tableSuffixOf(modelId), ids);
        return DataVo.success("删除成功");
    }

    public DataVo updateStatus(Long modelId, List<Long> ids, int status) {
        modelDataDao.updateStatus(tableSuffixOf(modelId), ids, status);
        return DataVo.success("状态已更新");
    }

    /**
     * 附件字段展开：IMAGE/FILE → 字段名+"Url"（img_url 字符串）；
     * IMAGES/FILES → 字段名+"Urls"（img_url 列表）。前台模板零二次查询（手册 §7.3）。
     */
    public void expandAttachments(Long modelId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        normalizeKeys(rows);
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.addAll(collectAttachmentIds(fields, row));
        }
        Map<Long, String> urlMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> img : modelDataDao.findImageUrls(new ArrayList<>(ids))) {
                long id = ((Number) img.get("id")).longValue();
                Object url = img.get("imgUrl");
                urlMap.put(id, url == null ? "" : String.valueOf(url));
                Object name = img.get("imgName");
                nameMap.put(id, name == null ? "" : String.valueOf(name));
            }
        }
        for (Map<String, Object> row : rows) {
            for (ModelField f : fields) {
                FieldTypeEnum type;
                try {
                    type = FieldTypeEnum.of(f.getFieldType());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!type.isAttachment()) {
                    continue;
                }
                Object v = row.get(f.getFieldName());
                if (v == null) {
                    continue;
                }
                if (type == FieldTypeEnum.IMAGE || type == FieldTypeEnum.FILE) {
                    long id = Long.parseLong(String.valueOf(v));
                    row.put(f.getFieldName() + "Url", urlMap.getOrDefault(id, ""));
                } else {
                    List<String> urls = new ArrayList<>();
                    for (String s : parseStringArray(String.valueOf(v))) {
                        try {
                            urls.add(urlMap.getOrDefault(Long.parseLong(s), ""));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    row.put(f.getFieldName() + "Urls", urls);
                }
            }
        }
    }

    /**
     * 行键名规范化：为含下划线的键（short_url/count_view/file_size/os_require…）
     * 追加驼峰别名（shortUrl/countView/fileSize/osRequire…），原键保留——
     * 模板统一用驼峰（与 Article 等 Java bean 一致），自定义字段原名不受影响。
     */
    private void normalizeKeys(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Map<String, Object> aliases = new HashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String key = e.getKey();
                if (key != null && key.indexOf('_') >= 0) {
                    StringBuilder sb = new StringBuilder();
                    boolean up = false;
                    for (char c : key.toCharArray()) {
                        if (c == '_') {
                            up = true;
                        } else {
                            sb.append(up ? Character.toUpperCase(c) : c);
                            up = false;
                        }
                    }
                    Object aliasValue = e.getValue();
                    if (key.endsWith("_time") && aliasValue instanceof String sv) {
                        // datetime 列在 map 行里是 ISO 字符串，统一为 "yyyy-MM-dd HH:mm"
                        String norm = sv.replace('T', ' ');
                        aliasValue = norm.length() > 16 ? norm.substring(0, 16) : norm;
                    }
                    aliases.put(sb.toString(), aliasValue);
                }
            }
            row.putAll(aliases);
        }
    }

    // /////////////////// 内部：元数据驱动校验与值转换 ///////////////////

    /**
     * 遍历字段定义校验 form 值并写入 values/columns（列名过白名单）。
     * refIds 收集附件引用（fly_images.id，含 JSON 数组内 id）。
     */
    private DataVo buildDynamicValues(List<ModelField> fields, Map<String, String> form,
                                      Map<String, Object> values, List<String> columns, List<Long> refIds) {
        for (ModelField f : fields) {
            String raw = form.get(f.getFieldName());
            boolean blank = StringUtils.isBlank(raw);
            if (blank && StringUtils.isNotBlank(f.getDefaultValue())) {
                raw = f.getDefaultValue();
                blank = false;
            }
            if (blank) {
                if (f.getIsRequired() == 1) {
                    return DataVo.failure(f.getFieldLabel() + "不能为空");
                }
                continue;
            }
            FieldTypeEnum type = FieldTypeEnum.of(f.getFieldType());
            Object v;
            try {
                switch (type) {
                    case NUMBER:
                        v = Long.parseLong(raw.trim());
                        break;
                    case DECIMAL:
                        v = new java.math.BigDecimal(raw.trim());
                        break;
                    case DATE:
                        v = new SimpleDateFormat("yyyy-MM-dd").parse(raw.trim());
                        break;
                    case DATETIME:
                        v = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(raw.trim().replace('T', ' '));
                        break;
                    case CHECKBOX:
                    case IMAGES:
                    case FILES: {
                        List<String> arr = parseStringArray(raw);
                        if (arr.isEmpty() && f.getIsRequired() == 1) {
                            return DataVo.failure(f.getFieldLabel() + "不能为空");
                        }
                        if (type == FieldTypeEnum.CHECKBOX && f.getOptions() != null) {
                            List<String> opts = parseStringArray(f.getOptions());
                            for (String a : arr) {
                                if (!opts.contains(a)) {
                                    return DataVo.failure(f.getFieldLabel() + "含有非法选项：" + a);
                                }
                            }
                        }
                        if (type.isAttachment() && !arr.isEmpty()) {
                            List<Long> ids = new ArrayList<>();
                            for (String a : arr) {
                                ids.add(Long.parseLong(a));
                            }
                            if (!modelDataDao.existsImages(ids)) {
                                return DataVo.failure(f.getFieldLabel() + "引用的附件不存在");
                            }
                            refIds.addAll(ids);
                        }
                        v = JSON.toJSONString(arr);
                        break;
                    }
                    case IMAGE:
                    case FILE: {
                        long aid = Long.parseLong(raw.trim());
                        if (!modelDataDao.existsImages(Arrays.asList(aid))) {
                            return DataVo.failure(f.getFieldLabel() + "引用的附件不存在");
                        }
                        refIds.add(aid);
                        v = aid;
                        break;
                    }
                    case SELECT:
                    case RADIO: {
                        if (f.getOptions() != null && !parseStringArray(f.getOptions()).contains(raw)) {
                            return DataVo.failure(f.getFieldLabel() + "含有非法选项：" + raw);
                        }
                        v = raw;
                        break;
                    }
                    default: {
                        // input/textarea/editor(走content通道)/region
                        if (!blank && f.getMaxlength() != null && raw.length() > f.getMaxlength()) {
                            return DataVo.failure(f.getFieldLabel() + "长度不能超过" + f.getMaxlength());
                        }
                        if (type == FieldTypeEnum.INPUT && StringUtils.isNotBlank(f.getRegex())
                                && !raw.matches(f.getRegex())) {
                            return DataVo.failure(f.getFieldLabel() + "格式不正确");
                        }
                        v = raw;
                    }
                }
            } catch (ParseException | IllegalArgumentException e) {
                return DataVo.failure(f.getFieldLabel() + "格式不正确");
            } catch (Exception e) {
                return DataVo.failure(f.getFieldLabel() + "处理失败");
            }
            if (type != FieldTypeEnum.EDITOR) {
                putColumn(columns, values, f.getFieldName(), v);
            }
        }
        return DataVo.success("ok");
    }

    private void putColumn(List<String> columns, Map<String, Object> values, String name, Object v) {
        if (v == null) {
            return;
        }
        if (v instanceof String && StringUtils.isBlank((String) v)) {
            return;
        }
        // 固有列（title/content 等）由代码写死，用允许固有列的校验；自定义字段已在 buildDynamicValues 过 safeColumnName
        try {
            columns.add(SqlSafeUtil.safeOrderColumn(name));
        } catch (IllegalArgumentException e) {
            return;
        }
        values.put(name, v);
    }

    private List<String> parseStringArray(String json) {
        try {
            if (json == null) {
                return new ArrayList<>();
            }
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            // 兼容逗号分隔
            return new ArrayList<>(Arrays.asList(json.split(",")));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> collectAttachmentIds(List<ModelField> fields, Map<String, Object> row) {
        List<Long> ids = new ArrayList<>();
        for (ModelField f : fields) {
            FieldTypeEnum type = FieldTypeEnum.of(f.getFieldType());
            if (!type.isAttachment()) {
                continue;
            }
            Object v = row.get(f.getFieldName());
            if (v == null) {
                continue;
            }
            try {
                if (type == FieldTypeEnum.IMAGE || type == FieldTypeEnum.FILE) {
                    ids.add(Long.parseLong(String.valueOf(v)));
                } else {
                    for (String s : parseStringArray(String.valueOf(v))) {
                        ids.add(Long.parseLong(s));
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private List<Long> distinct(List<Long> ids) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(ids));
    }

    private void adjustRefCounts(List<Long> oldRefs, List<Long> newRefs) {
        List<Long> toDec = new ArrayList<>(oldRefs);
        toDec.removeAll(newRefs);
        List<Long> toInc = new ArrayList<>(newRefs);
        toInc.removeAll(oldRefs);
        if (!toDec.isEmpty()) {
            modelDataDao.decrImagesRefCount(toDec);
        }
        if (!toInc.isEmpty()) {
            modelDataDao.incrImagesRefCount(toInc);
        }
    }

    /**
     * 表名后缀解析：modelId → model.code（D7）。
     *
     * <p>对外所有方法仍接收 {@code Long modelId}（已上线的 vben 前端契约
     * {@code /system/modelData/list/{modelId}} 不便变动），内部统一经此方法解析为
     * 物理表名后缀 {@code fly_cmodel_{code}}。表名不再是雪花 ID。
     *
     * <p>额外走一次 {@link SqlSafeUtil#safeTableSuffix(String)}：模型 code 在创建时已校验，
     * 但缓存/直连库的脏数据仍可能绕过，动态 SQL 入口再校验一次是纵深防御（D10）。
     *
     * @param modelId 模型主键
     * @return 模型 code（物理表名后缀）
     * @throws IllegalArgumentException 模型不存在或 code 非法
     */
    private String tableSuffixOf(Long modelId) {
        Model model = modelService.findModelById(modelId);
        if (model == null || StringUtils.isBlank(model.getCode())) {
            throw new IllegalArgumentException("模型不存在：" + modelId);
        }
        return SqlSafeUtil.safeTableSuffix(model.getCode());
    }

    private String genShortUrl(Long modelId) {
        String code;
        for (String s : ShortUrlUtils.shortUrl(null)) {
            code = s;
            if (!modelDataDao.existsShortUrl(tableSuffixOf(modelId), code)) {
                return code;
            }
        }
        return String.valueOf(System.currentTimeMillis()).substring(3);
    }
}
