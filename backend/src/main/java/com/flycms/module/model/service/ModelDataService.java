package com.flycms.module.model.service;

import com.alibaba.fastjson2.JSON;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ModelDataService.class);

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
        pageVo.setList(modelDataDao.selectPage(tableSuffixOf(modelId), whereSql, orderBySql,
                listColumns(fields), params));
        pageVo.setCount(modelDataDao.countPage(tableSuffixOf(modelId), whereSql, params));
        return pageVo;
    }

    /**
     * 列表查询的固定列（与 {@link ModelTableService#createModelTable} 的建表 DDL 一致）。
     * 顺序即 SELECT 输出顺序，便于人工比对。
     */
    private static final List<String> LIST_BASE_COLUMNS = Arrays.asList(
            "id", "short_url", "user_id", "category_id", "title", "keywords", "description",
            "thumbnail", "recommend", "count_view", "count_comment", "status",
            "create_time", "update_time", "publish_time");

    /**
     * G11：列表列白名单投影（收敛 {@code SELECT *}）。
     *
     * <p>组成 = 固定列 {@link #LIST_BASE_COLUMNS} + 启用模型字段中「有独立列且非 text 系」的字段。
     * 排除 text 系（{@link FieldTypeEnum#TEXTAREA}）与主表 {@code content} 通道：列表页模板只消费
     * 标题 / 摘要 / 封面 / 时间 / 计数等轻量列，正文一律由详情查询
     * （{@link #findDataById} / {@link #findByShortUrl}，保留 {@code SELECT *}）提供，
     * 避免列表查询把 longtext / text 整列拉回内存。
     *
     * <p>列名一律过 {@link SqlSafeUtil#safeColumnName}，非法字段名直接跳过（不进入 SQL）。
     */
    private List<String> listColumns(List<ModelField> fields) {
        List<String> columns = new ArrayList<>(LIST_BASE_COLUMNS);
        for (ModelField f : fields) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            // editor 不建列（正文走 content 通道）；textarea 是重列，列表不投影
            if (!type.hasColumn() || type.isHeavyText()) {
                continue;
            }
            String name;
            try {
                name = SqlSafeUtil.safeColumnName(f.getFieldName());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!columns.contains(name)) {
                columns.add(name);
            }
        }
        return columns;
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
        DataVo check = buildDynamicValues(fields, form, values, columns, refIds, tableSuffixOf(modelId));
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
        DataVo check = buildDynamicValues(fields, form, values, columns, newRefs, tableSuffixOf(modelId));
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
        // E1：同一个入口顺带展开关联引用，调用方无需改动
        expandReferences(tableSuffixOf(modelId), fields, rows);
    }

    /**
     * E1 关联展开：RELATE → 字段名+"Obj"（目标内容 Map）；RELATES → 字段名+"List"（目标内容 List）。
     *
     * <p>与 {@link #expandAttachments} 同构，但目标不是 `fly_images` 而是另一个模型数据表。
     * 目标表由 {@link ModelField#getRelateModel()} 决定，留空表示本模型（自关联树，如回答的 parent_id）。
     * <b>一次批量查询</b>满足"模板零二次查询"（模型手册 §7.3）：<br>
     * {@code ${(item.brand_idObj.title)!''}} / {@code <#list item.tag_idList as t>${t.title}}</#list>}
     *
     * <p>悬空引用（目标已删）不抛异常，直接不放入结果 —— 模板侧用 {@code ??} 判空降级。
     */
    private void expandReferences(String selfSuffix, List<ModelField> fields, List<Map<String, Object>> rows) {
        for (ModelField f : fields) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!type.isRelation()) {
                continue;
            }
            String target = referenceTargetSuffix(f, selfSuffix);
            if (target == null) {
                continue;
            }
            // 第一遍：按行解析出引用的 id
            Map<Map<String, Object>, List<Long>> perRow = new java.util.IdentityHashMap<>();
            java.util.Set<Long> all = new java.util.LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                List<Long> ids = readReferenceIds(type, f, row);
                if (ids.isEmpty()) {
                    continue;
                }
                perRow.put(row, ids);
                all.addAll(ids);
            }
            if (all.isEmpty()) {
                continue;
            }
            // 第二遍：一次批量取回目标行（只取展示列）
            List<Map<String, Object>> targets =
                    modelDataDao.findRowsByIds(target, new ArrayList<>(all));
            normalizeKeys(targets);
            Map<Long, Map<String, Object>> byId = new HashMap<>();
            for (Map<String, Object> t : targets) {
                Object tid = t.get("id");
                if (tid != null) {
                    byId.put(Long.parseLong(String.valueOf(tid)), t);
                }
            }
            for (Map.Entry<Map<String, Object>, List<Long>> e : perRow.entrySet()) {
                Map<String, Object> row = e.getKey();
                if (type == FieldTypeEnum.RELATE) {
                    row.put(f.getFieldName() + "Obj", byId.get(e.getValue().get(0)));
                } else {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Long id : e.getValue()) {
                        Map<String, Object> t = byId.get(id);
                        if (t != null) {
                            list.add(t);
                        }
                    }
                    row.put(f.getFieldName() + "List", list);
                }
            }
        }
    }

    /** 读取一行里某关联字段引用的 id 列表（RELATE 单值 / RELATES JSON 数组） */
    private List<Long> readReferenceIds(FieldTypeEnum type, ModelField f, Map<String, Object> row) {
        Object v = row.get(f.getFieldName());
        List<Long> ids = new ArrayList<>();
        if (v == null) {
            return ids;
        }
        try {
            if (type == FieldTypeEnum.RELATE) {
                ids.add(Long.parseLong(String.valueOf(v)));
            } else {
                for (String s : parseStringArray(String.valueOf(v))) {
                    ids.add(Long.parseLong(s.trim()));
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return ids;
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
     *
     * @param selfSuffix 本模型的物理表后缀，供 RELATE 自关联（relate_model 留空）解析目标表
     */
    private DataVo buildDynamicValues(List<ModelField> fields, Map<String, String> form,
                                      Map<String, Object> values, List<String> columns, List<Long> refIds,
                                      String selfSuffix) {
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
                    case RELATE: {
                        // E1 单值关联：目标内容必须存在且 status=1（防悬空引用）
                        long rid;
                        try {
                            rid = Long.parseLong(raw.trim());
                        } catch (NumberFormatException e) {
                            return DataVo.failure(f.getFieldLabel() + "引用格式不正确");
                        }
                        DataVo rel = checkReferences(f, selfSuffix, Arrays.asList(rid));
                        if (rel.getCode() != DataVo.CODE_SUCCESS) {
                            return rel;
                        }
                        v = rid;
                        break;
                    }
                    case RELATES: {
                        // E1 多值关联：存 id 数组（JSON），逐个校验存在且已发布
                        List<String> arr = parseStringArray(raw);
                        if (arr.isEmpty() && f.getIsRequired() == 1) {
                            return DataVo.failure(f.getFieldLabel() + "不能为空");
                        }
                        List<Long> ids = new ArrayList<>();
                        for (String a : arr) {
                            try {
                                ids.add(Long.parseLong(a.trim()));
                            } catch (NumberFormatException e) {
                                return DataVo.failure(f.getFieldLabel() + "含有非法引用：" + a);
                            }
                        }
                        if (!ids.isEmpty()) {
                            DataVo rel = checkReferences(f, selfSuffix, distinct(ids));
                            if (rel.getCode() != DataVo.CODE_SUCCESS) {
                                return rel;
                            }
                        }
                        v = JSON.toJSONString(arr);
                        break;
                    }
                    case IMAGE_URL:
                    case FILE_URL: {
                        // E3 直存 URL：不参与引用计数；值会直接输出到 src/href，必须拒绝可闭合属性的字符
                        String url = raw.trim();
                        if (url.indexOf('<') >= 0 || url.indexOf('>') >= 0
                                || url.indexOf('"') >= 0 || url.indexOf('\'') >= 0) {
                            return DataVo.failure(f.getFieldLabel() + "含非法字符（< > \" '）");
                        }
                        v = url;
                        break;
                    }
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
                            if (modelDataDao.countImages(ids) < ids.size()) {
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
                        if (modelDataDao.countImages(Arrays.asList(aid)) == 0) {
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
                // 动态模型字段处理链路的异常必须留痕，否则只能看到笼统的"处理失败"
                log.error("模型字段[{}]({})处理失败: {}", f.getFieldName(), f.getFieldType(), e.getMessage(), e);
                return DataVo.failure(f.getFieldLabel() + "处理失败");
            }
            if (type != FieldTypeEnum.EDITOR) {
                putColumn(columns, values, f.getFieldName(), v);
            }
        }
        return DataVo.success("ok");
    }

    /**
     * E1：校验关联引用的目标内容是否全部存在且已发布（status=1）。
     * 目标表由 {@link ModelField#getRelateModel()} 决定；留空表示关联本模型（自关联树）。
     */
    private DataVo checkReferences(ModelField f, String selfSuffix, List<Long> ids) {
        String target = referenceTargetSuffix(f, selfSuffix);
        if (target == null) {
            return DataVo.failure(f.getFieldLabel() + "的关联目标模型标识非法，请检查字段配置");
        }
        List<Map<String, Object>> rows = modelDataDao.findRowsByIds(target, ids);
        long published = 0;
        for (Map<String, Object> r : rows) {
            if (toInt(r.get("status")) == 1) {
                published++;
            }
        }
        if (published < ids.size()) {
            return DataVo.failure(f.getFieldLabel() + "引用的内容不存在或未发布");
        }
        return DataVo.success("ok");
    }

    /**
     * E1：解析关联字段的目标表后缀。relate_model 留空 → 本模型（自关联树）；
     * 否则按「存量表后缀豁免」白名单校验（与 ModelTableService 同口径）。
     */
    private String referenceTargetSuffix(ModelField f, String selfSuffix) {
        String code = f.getRelateModel();
        if (StringUtils.isBlank(code)) {
            return selfSuffix;
        }
        try {
            return SqlSafeUtil.safeTableSuffixForExisting(code);
        } catch (IllegalArgumentException e) {
            log.warn("字段[{}]的关联目标模型标识非法，已拒绝：{}", f.getFieldName(), code);
            return null;
        }
    }

    private int toInt(Object v) {
        if (v == null) {
            return -1;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
     * <p>额外走一次 {@link SqlSafeUtil#safeTableSuffixForExisting(String)}：模型 code 在创建时已校验，
     * 但缓存/直连库的脏数据仍可能绕过，动态 SQL 入口再校验一次是纵深防御（D10）。
     * 读取/维护路径用存量豁免版本（只拦 SQL 保留字）：种子模型 images 命中项目表名黑名单，
     * 若按全量黑名单校验会让整条管理链路 500（2026-09-27 修复）。
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
        return SqlSafeUtil.safeTableSuffixForExisting(model.getCode());
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
