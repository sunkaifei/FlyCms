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

    /** P2 Rollup 聚合函数白名单（与 ModelFieldService 校验一致） */
    private static final java.util.Set<String> ROLLUP_FUNCS = java.util.Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

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
    @Autowired
    private com.flycms.module.comment.dao.CommentDao commentDao;
    @Autowired
    private com.flycms.module.user.service.UserService userService;
    @Autowired
    private com.flycms.module.images.service.ImagesService imagesService;
    @Autowired
    private com.flycms.module.config.service.ConfigService configService;
    @Autowired
    private com.flycms.module.model.dao.ContentVersionDao contentVersionDao;
    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

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
        return selectPage(modelId, title, categoryId, status, filters, orderby, order,
                page, rows, notId, front, null);
    }

    /**
     * 带 userId 筛选的重载（U3）：固定列 user_id 过滤，供用户中心「他的文章」类页面使用。
     * 旧签名等价于 userId=null。此前模板参数 user_id 落不进任何 is_filter 字段被静默忽略
     * （U2 people 页潜伏 bug），改由该显式参数承接。
     */
    public PageVo<Map<String, Object>> selectPage(Long modelId, String title, Long categoryId, Integer status,
                                                  Map<String, String> filters, String orderby, String order,
                                                  int page, int rows, Long notId, boolean front, Long userId) {
        return selectPage(modelId, title, categoryId, status, filters, orderby, order,
                page, rows, notId, front, userId, null, null, null);
    }

    /**
     * E8 时间窗过滤重载：timeField 限定为固有时间列或本模型 date/datetime 类型字段，
     * timeFrom/timeTo 接受 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（非法格式静默忽略，不阻断查询）。
     * 公告生效期这类场景：<@fly_page_model model="announcements" timeField="start_time"
     *   timeFrom="${.now?string('yyyy-MM-dd HH:mm:ss')}"。
     */
    public PageVo<Map<String, Object>> selectPage(Long modelId, String title, Long categoryId, Integer status,
                                                  Map<String, String> filters, String orderby, String order,
                                                  int page, int rows, Long notId, boolean front, Long userId,
                                                  String timeField, String timeFrom, String timeTo) {
        return selectPage(modelId, title, categoryId, status, filters, orderby, order,
                page, rows, notId, front, userId, timeField, timeFrom, timeTo, false);
    }

    /**
     * W/X 批次：withContent=true 时列表投影追加主表 content 列——问答频道详情页需内联
     * 渲染回答正文（content 走主表通道且默认被 G11 投影排除），由模板显式开启，
     * 其余列表页保持轻量投影不变。
     */
    public PageVo<Map<String, Object>> selectPage(Long modelId, String title, Long categoryId, Integer status,
                                                  Map<String, String> filters, String orderby, String order,
                                                  int page, int rows, Long notId, boolean front, Long userId,
                                                  String timeField, String timeFrom, String timeTo,
                                                  boolean withContent) {
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
        if (userId != null && userId > 0) {
            where.add("user_id = #{params.userId}");
            params.put("userId", userId);
        }
        // E8 时间窗（公告生效期等）：timeField 白名单 = 固有时间列 + 本模型 date/datetime 字段
        if (StringUtils.isNotBlank(timeField) && (StringUtils.isNotBlank(timeFrom) || StringUtils.isNotBlank(timeTo))) {
            boolean allowed = "create_time".equals(timeField) || "update_time".equals(timeField)
                    || "publish_time".equals(timeField);
            if (!allowed) {
                try {
                    if (SqlSafeUtil.safeColumnName(timeField).equals(timeField)) {
                        for (ModelField f : topFields(fields)) {
                            if (timeField.equals(f.getFieldName())
                                    && ("date".equals(f.getFieldType()) || "datetime".equals(f.getFieldType()))) {
                                allowed = true;
                                break;
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // 非法列名保持 allowed=false
                }
            }
            if (allowed) {
                String col = "`" + timeField + "`";
                if (StringUtils.isNotBlank(timeFrom) && parseTime(timeFrom) != null) {
                    where.add(col + " >= #{params.timeFrom}");
                    params.put("timeFrom", timeFrom + (timeFrom.length() == 10 ? " 00:00:00" : ""));
                }
                if (StringUtils.isNotBlank(timeTo) && parseTime(timeTo) != null) {
                    where.add(col + " <= #{params.timeTo}");
                    params.put("timeTo", timeTo + (timeTo.length() == 10 ? " 23:59:59" : ""));
                }
            }
        }
        // 自定义字段筛选：仅 is_filter=1 且值非空（P1：结构子字段不占物理列，只遍历顶层字段）
        for (ModelField f : topFields(fields)) {
            if (f.getIsFilter() != 1) {
                continue;
            }
            String v = filters == null ? null : filters.get(f.getFieldName());
            if (StringUtils.isBlank(v)) {
                continue;
            }
            // W 时间范围字段：筛选值 "start,end" → 重叠命中（存起<=筛止 AND 存止>=筛起）
            if ("date_range".equals(f.getFieldType()) || "datetime_range".equals(f.getFieldType())) {
                String[] parts = v.split("[,;，；]", 2);
                if (parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
                    continue;
                }
                String fs = parts[0].trim();
                String fe = parts[1].trim();
                boolean isDate = "date_range".equals(f.getFieldType());
                if (!isDate) {
                    // datetime 补齐到秒，保证与存储值字典序可比
                    if (fs.length() == 10) {
                        fs = fs + " 00:00:00";
                    } else if (fs.length() == 16) {
                        fs = fs + ":00";
                    }
                    if (fe.length() == 10) {
                        fe = fe + " 23:59:59";
                    } else if (fe.length() == 16) {
                        fe = fe + ":00";
                    }
                }
                String colName = "`" + SqlSafeUtil.safeColumnName(f.getFieldName()) + "`";
                where.add("CAST(JSON_UNQUOTE(JSON_EXTRACT(" + colName + ",'$[1]')) AS CHAR) >= #{params."
                        + f.getFieldName() + "_s}");
                where.add("CAST(JSON_UNQUOTE(JSON_EXTRACT(" + colName + ",'$[0]')) AS CHAR) <= #{params."
                        + f.getFieldName() + "_e}");
                params.put(f.getFieldName() + "_s", fs);
                params.put(f.getFieldName() + "_e", fe);
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
        List<String> columns = listColumns(fields);
        if (withContent && !columns.contains("content")) {
            columns.add("content");
        }
        pageVo.setList(modelDataDao.selectPage(tableSuffixOf(modelId), whereSql, orderBySql,
                columns, params));
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
        // P1：只遍历顶层字段——子字段存父字段 JSON 内，没有物理列，投影会直接 SQL 报错
        for (ModelField f : topFields(fields)) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            // editor 不建列（正文走 content 通道）；textarea 是重列，列表不投影；
            // P1 结构列（group/repeater 的 JSON）可能是大对象，列表同样不投影
            if (!type.hasColumn() || type.isHeavyText() || type.isStructure()) {
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

    // /////////////////// X 批次：前台互动闭环 ///////////////////

    /**
     * X1 投稿上下文预置的行校验：目标模型下该行存在且已发布（status=1），返回行（含 title
     * 供只读条展示）；模型/行不存在或未发布返回 null。调用方决定 404/失败语义。
     */
    public Map<String, Object> findPublishedRow(String code, Long id) {
        Model model = modelService.findModelByCode(code);
        if (model == null) {
            return null;
        }
        Map<String, Object> row = modelDataDao.findDataById(tableSuffixOf(model.getId()), id);
        if (row == null) {
            return null;
        }
        normalizeKeys(List.of(row));
        Object status = row.get("status");
        if (status == null || !"1".equals(String.valueOf(status))) {
            return null;
        }
        return row;
    }

    /**
     * X2 内容所有者操作：登录用户更新 user_id=自己的内容行的白名单字段。
     *
     * <p>白名单口径：is_form=1 的非关联自定义字段（relate/relates/m2a 关联边所有者不可
     * 改——防把回答挪到别的问题；editor 走主表 content 通道不在动态列；虚拟字段不落列）。
     * 硬约束：行必须存在且 user_id=操作者；status/title 等固有列不在白名单（不绕审核流）。
     * 落库复用 convertFieldValue 全量校验，附件引用计数调整 + G12 快照 + G18 事件与
     * updateData 同口径。
     */
    public DataVo ownerUpdate(Long modelId, Long id, Map<String, String> form, Long userId) {
        Model model = modelService.findModelById(modelId);
        if (model == null || model.getStatus() != 1) {
            return DataVo.failure("模型不存在或已禁用");
        }
        Map<String, Object> row = modelDataDao.findDataById(tableSuffixOf(modelId), id);
        if (row == null) {
            return DataVo.failure("内容不存在");
        }
        normalizeKeys(List.of(row));
        Object owner = row.get("userId");
        if (owner == null || !String.valueOf(owner).equals(String.valueOf(userId))) {
            return DataVo.failure("只能操作自己发布的内容");
        }
        String selfSuffix = tableSuffixOf(modelId);
        List<ModelField> fields = topFields(modelFieldDao.findFieldsByModelId(modelId, 1));
        List<String> columns = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        List<Long> newRefs = new ArrayList<>();
        boolean changed = false;
        for (ModelField f : fields) {
            if (f.getIsForm() == 0) {
                continue;
            }
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (type.isVirtual() || type == FieldTypeEnum.EDITOR
                    || type == FieldTypeEnum.RELATE || type == FieldTypeEnum.RELATES
                    || type == FieldTypeEnum.M2A) {
                continue;
            }
            String raw = form.get(f.getFieldName());
            if (raw == null) {
                continue;
            }
            if (StringUtils.isBlank(raw)) {
                if (f.getIsRequired() == 1) {
                    return DataVo.failure(f.getFieldLabel() + "不能为空");
                }
                columns.add(f.getFieldName());
                values.put(f.getFieldName(), null);
                changed = true;
                continue;
            }
            Object v;
            try {
                v = convertFieldValue(f, type, raw, newRefs, selfSuffix);
            } catch (IllegalArgumentException e) {
                return DataVo.failure(StringUtils.defaultIfBlank(e.getMessage(), f.getFieldLabel() + "格式不正确"));
            } catch (Exception e) {
                log.error("所有者更新字段[{}]({})失败: {}", f.getFieldName(), f.getFieldType(), e.getMessage(), e);
                return DataVo.failure(f.getFieldLabel() + "处理失败");
            }
            putColumn(columns, values, f.getFieldName(), v);
            changed = true;
        }
        if (!changed) {
            return DataVo.failure("没有可更新的字段");
        }
        // 附件引用计数调整（旧引用取整行附件字段，与 updateData 同口径）
        List<Long> oldRefs = new ArrayList<>();
        for (ModelField f : fields) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (type.isAttachment()) {
                oldRefs.addAll(readReferenceIds(type, f, row));
            }
        }
        modelDataDao.updateData(selfSuffix, id, columns, values);
        adjustRefCounts(oldRefs, distinct(newRefs));
        snapshotContentVersion(model, id, userId, "所有者更新");
        publishContentEvent("update", model, List.of(id), userId);
        return DataVo.success("更新成功");
    }

    // /////////////////// 写入 ///////////////////

    public DataVo insertData(Long modelId, Map<String, String> form, Long userId) {
        return insertData(modelId, form, userId, null);
    }

    /** G12：editorId 供版本快照记录操作人 */
    public DataVo insertData(Long modelId, Map<String, String> form, Long userId, Long editorId) {
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
        Map<String, List<Map<String, Object>>> m2aOut = new HashMap<>();
        DataVo check = buildDynamicValues(fields, form, values, columns, refIds, m2aOut, tableSuffixOf(modelId));
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        DataVo uniq = checkUnique(fields, values, tableSuffixOf(modelId), null);
        if (uniq.getCode() != DataVo.CODE_SUCCESS) {
            return uniq;
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
        // Q3 内容图片本地化：模型开启后，编辑器外站图抓取到本地并替换地址
        putColumn(columns, values, "content",
                localizeContentIfEnabled(model, form.get("content"), userId != null ? userId : editorId));
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
        // G12：初版快照（v1，DB 为事实源，快照即版本）
        snapshotContentVersion(model, id, editorId, null);
        // G18：内容新建事件
        publishContentEvent("insert", model, List.of(id), userId != null ? userId : editorId);
        if (!refIds.isEmpty()) {
            modelDataDao.incrImagesRefCount(distinct(refIds));
        }
        // P2 M2A：内容行落库后同步中间表关系（覆盖式）
        syncRelations(model, id, m2aOut);
        return DataVo.success("发布成功", id);
    }

    public DataVo updateData(Long modelId, Long id, Map<String, String> form) {
        return updateData(modelId, id, form, null);
    }

    /** G12：editorId 供版本快照记录操作人 */
    public DataVo updateData(Long modelId, Long id, Map<String, String> form, Long editorId) {
        Model model = modelService.findModelById(modelId);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
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
        Map<String, List<Map<String, Object>>> m2aOut = new HashMap<>();
        DataVo check = buildDynamicValues(fields, form, values, columns, newRefs, m2aOut, tableSuffixOf(modelId));
        if (check.getCode() != DataVo.CODE_SUCCESS) {
            return check;
        }
        DataVo uniq = checkUnique(fields, values, tableSuffixOf(modelId), id);
        if (uniq.getCode() != DataVo.CODE_SUCCESS) {
            return uniq;
        }
        values.put("id", id);
        putColumn(columns, values, "title", form.get("title"));
        putColumn(columns, values, "keywords", form.get("keywords"));
        putColumn(columns, values, "description", form.get("description"));
        // Q3 内容图片本地化：模型开启后，编辑器外站图抓取到本地并替换地址
        putColumn(columns, values, "content",
                localizeContentIfEnabled(model, form.get("content"), editorId));
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
        // P2 M2A：覆盖式同步（未传该字段则不触碰既有关系）
        syncRelations(model, id, m2aOut);
        // G12：每次更新落一版快照（version+1）
        snapshotContentVersion(model, id, editorId, null);
        // G18：内容更新事件
        publishContentEvent("update", model, List.of(id), editorId);
        return DataVo.success("更新成功");
    }

    /**
     * 软删（status=3）+ 引用计数 -1
     */
    public DataVo deleteData(Long modelId, List<Long> ids) {
        Model model = modelService.findModelById(modelId);
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
        // P2 M2A：内容删除时清理其发出的全部任意关系
        if (model != null && model.getCode() != null) {
            modelDataDao.deleteRelationsByFromIds(model.getCode(), ids);
        }
        // E4 平台评论：内容删除联动清理其全部评论（DAO 直连避免 service 环依赖）
        if (model != null && model.getCode() != null) {
            commentDao.deleteByTarget(model.getCode(), ids);
        }
        // G18：内容删除事件
        publishContentEvent("delete", model, ids, null);
        return DataVo.success("删除成功");
    }

    public DataVo updateStatus(Long modelId, List<Long> ids, int status) {
        modelDataDao.updateStatus(tableSuffixOf(modelId), ids, status);
        // G12：状态切换留痕（发布/下架/审核动作都进版本历史）
        Model model = modelService.findModelById(modelId);
        if (model != null) {
            for (Long id : ids) {
                snapshotContentVersion(model, id, null, "状态切换→" + status);
            }
        }
        // G18：状态变更事件
        publishContentEvent("status", model, ids, null);
        return DataVo.success("状态已更新");
    }

    /**
     * G12 版本快照：整行 SELECT * 序列化落 {@code fly_content_version}（version=max+1）。
     * 快照失败只告警不阻断主流程（版本能力是增强，不是内容写入的前置）。
     */
    private void snapshotContentVersion(Model model, Long id, Long editorId, String remark) {
        try {
            Map<String, Object> row = modelDataDao.findDataById(tableSuffixOf(model.getId()), id);
            if (row == null) {
                return;
            }
            com.flycms.module.model.model.ContentVersion v =
                    new com.flycms.module.model.model.ContentVersion();
            v.setId(SnowFlake.getInstance().nextId());
            v.setTargetModel(model.getCode());
            v.setTargetId(id);
            v.setVersion(contentVersionDao.maxVersion(model.getCode(), id) + 1);
            v.setContentJson(JSON.toJSONString(row));
            Object st = row.get("status");
            v.setStatus(st == null ? null : Integer.parseInt(String.valueOf(st)));
            v.setEditorId(editorId);
            v.setRemark(remark);
            v.setCreateTime(new Date());
            contentVersionDao.addVersion(v);
        } catch (Exception e) {
            log.warn("内容版本快照失败：{}#{} {}", model.getCode(), id, e.getMessage());
        }
    }

    /**
     * G12 版本对比：from/to 两版快照的逐字段差异（仅列出有变化的列；id/short_url 除外）。
     * 返回 [{field, from, to}]（值统一字符串化；时间列 ISO 形态）。
     */
    public List<Map<String, Object>> diffVersions(Long modelId, Long id, int fromVersion, int toVersion) {
        Model model = modelService.findModelById(modelId);
        List<Map<String, Object>> out = new ArrayList<>();
        if (model == null) {
            return out;
        }
        com.flycms.module.model.model.ContentVersion from =
                contentVersionDao.findVersion(model.getCode(), id, fromVersion);
        com.flycms.module.model.model.ContentVersion to =
                contentVersionDao.findVersion(model.getCode(), id, toVersion);
        if (from == null || to == null) {
            return out;
        }
        Map<String, Object> a = JSON.parseObject(from.getContentJson(),
                new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() { });
        Map<String, Object> b = JSON.parseObject(to.getContentJson(),
                new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() { });
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        for (String key : keys) {
            if ("id".equalsIgnoreCase(key) || "short_url".equalsIgnoreCase(key)) {
                continue;
            }
            Object av = a.get(key);
            Object bv = b.get(key);
            String as = av == null ? "" : String.valueOf(av);
            String bs = bv == null ? "" : String.valueOf(bv);
            if (!as.equals(bs)) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("field", key);
                d.put("from", as);
                d.put("to", bs);
                out.add(d);
            }
        }
        return out;
    }

    /**
     * G12 版本列表（新→旧，不含 content_json 大字段）。
     */
    public PageVo<com.flycms.module.model.model.ContentVersion> listVersions(
            Long modelId, Long id, int pageNum, int rows) {
        Model model = modelService.findModelById(modelId);
        PageVo<com.flycms.module.model.model.ContentVersion> pageVo =
                new PageVo<>(pageNum);
        pageVo.setRows(rows);
        if (model == null) {
            pageVo.setList(new ArrayList<>());
            return pageVo;
        }
        String code = model.getCode();
        pageVo.setCount(contentVersionDao.countVersions(code, id));
        pageVo.setList(contentVersionDao.selectVersions(code, id, pageVo.getOffset(), pageVo.getRows()));
        return pageVo;
    }

    /**
     * G12 恢复：快照整行写回主表（跳过 id/short_url），并把恢复动作另存为新版本——历史不丢。
     */
    public DataVo restoreVersion(Long modelId, Long id, int version, Long editorId) {
        Model model = modelService.findModelById(modelId);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        com.flycms.module.model.model.ContentVersion v =
                contentVersionDao.findVersion(model.getCode(), id, version);
        if (v == null) {
            return DataVo.failure("版本不存在");
        }
        Map<String, Object> row = modelDataDao.findDataById(tableSuffixOf(modelId), id);
        if (row == null) {
            return DataVo.failure("内容不存在或已删除，无法恢复");
        }
        Map<String, Object> snap;
        try {
            snap = JSON.parseObject(v.getContentJson(), new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            return DataVo.failure("版本快照损坏，无法恢复");
        }
        if (snap == null || snap.isEmpty()) {
            return DataVo.failure("版本快照为空，无法恢复");
        }
        // 恢复列校验：标识符正则即可——列名来自本表历史快照（非用户输入），
        // 不能用 safeColumnName（其黑名单含全部建表固定列，会把 title/status 等全拒掉）
        java.util.regex.Pattern IDENT = java.util.regex.Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
        List<String> columns = new ArrayList<>();
        List<String> setParts = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, Object> e : snap.entrySet()) {
            String key = e.getKey();
            if ("id".equalsIgnoreCase(key) || "short_url".equalsIgnoreCase(key)) {
                continue;
            }
            if (!IDENT.matcher(key).matches()) {
                continue;
            }
            String pName = "p" + (i++);
            // 占位符必须带 params. 前缀（多 @Param 方法绑定名是 params.pN，裸 pN 不在 ParamMap 顶层）
            setParts.add("`" + key + "` = #{params." + pName + "}");
            params.put(pName, e.getValue());
        }
        if (setParts.isEmpty()) {
            return DataVo.failure("版本快照无可恢复列");
        }
        contentVersionDao.restoreColumns(tableSuffixOf(modelId),
                String.join(", ", setParts), id, params);
        // 恢复动作本身也进版本历史
        snapshotContentVersion(model, id, editorId, "恢复自 v" + version);
        return DataVo.success("已恢复到 v" + version + "（另存为最新版本）");
    }

    /**
     * 平台评论计数（E4）：目标内容固有列 count_comment 增减，下限 0。
     * 列名/表名固定列 + safeTableSuffix 白名单，delta 由调用方给 ±1。
     */
    public void adjustCommentCount(String modelCode, Long id, int delta) {
        if (StringUtils.isBlank(modelCode) || id == null || id <= 0 || delta == 0) {
            return;
        }
        try {
            modelDataDao.adjustCommentCount(SqlSafeUtil.safeTableSuffix(modelCode), id,
                    delta > 0 ? 1 : -1);
        } catch (IllegalArgumentException e) {
            log.warn("评论计数调整：非法模型码 {}", modelCode);
        }
    }

    /**
     * G18：内容生命周期事件发布（insert/update/delete/status）。
     * 插件/自动化的稳定契约——本期只发布不消费；监听侧异常不阻断主流程。
     */
    private void publishContentEvent(String action, Model model, List<Long> ids, Long operatorId) {
        try {
            if (model != null && model.getCode() != null && ids != null && !ids.isEmpty()) {
                eventPublisher.publishEvent(new com.flycms.core.event.ContentChangedEvent(
                        this, action, model.getCode(), ids, operatorId));
            }
        } catch (Exception e) {
            log.warn("内容事件发布失败（不影响主流程）：{} {} {}", action, model == null ? "?" : model.getCode(), e.getMessage());
        }
    }

    /**
     * V4 聚合统计（fly_stats_model 标签用）：按固有维度分组计数，恒定过滤已发布。
     * by 白名单：category（分类树节点，name=分类名）/ author（发布者，name=昵称）。
     */
    public List<Map<String, Object>> statsGroup(com.flycms.module.model.model.Model model,
                                                String by, int status, int rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        String col;
        switch (StringUtils.defaultString(by, "category")) {
            case "author" -> col = "user_id";
            case "category" -> col = "category_id";
            default -> {
                return out;
            }
        }
        List<Map<String, Object>> groups =
                modelDataDao.statsGroup(tableSuffixOf(model.getId()), col, status);
        // 归一化输出键：statKey→key、statCount→count（模板/文档口径）
        for (Map<String, Object> g : groups) {
            g.put("key", g.remove("statKey"));
            g.put("count", g.remove("statCount"));
        }
        if ("author".equals(col)) {
            for (Map<String, Object> g : groups) {
                Object key = g.get("statKey");
                String name = "";
                try {
                    Long uid = key == null ? null : Long.parseLong(String.valueOf(key));
                    com.flycms.module.user.model.User u = uid == null ? null
                            : userService.findUserById(uid, 0);
                    if (u != null) {
                        name = StringUtils.defaultIfBlank(u.getNickName(), u.getUserName());
                    }
                } catch (Exception ignored) {
                    // 用户已删除等场景留空
                }
                g.put("name", name);
                out.add(g);
            }
            return out;
        }
        // category：解析分类名
        Map<String, com.flycms.module.model.model.ModelCategory> catById = new HashMap<>();
        for (com.flycms.module.model.model.ModelCategory c
                : modelCategoryService.findCategoriesByModelId(model.getId(), null)) {
            catById.put(String.valueOf(c.getId()), c);
        }
        for (Map<String, Object> g : groups) {
            String key = String.valueOf(g.get("statKey"));
            com.flycms.module.model.model.ModelCategory c = catById.get(key);
            g.put("name", c == null ? "未分类" : c.getName());
            out.add(g);
        }
        return out;
    }

    /** 按状态计数（投稿审核待审角标：跨启用模型求和用） */
    public int countByStatus(Long modelId, int status) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        return modelDataDao.countPage(tableSuffixOf(modelId), "status = #{params.status}", params);
    }

    /**
     * 读侧统一展开：附件（含 P1 结构体内嵌附件）→ E1 关联（含 P2 Lookup 列）→
     * P2 Rollup 聚合 → P2 M2A 任意关系 → P2 双向标注（反向引用）。
     * 全部一次批量查询，前台/后台模板零二次查询（模型手册 §7.3）。
     */
    public void expandAttachments(Long modelId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        normalizeKeys(rows);
        Model model = modelService.findModelById(modelId);
        if (model == null) {
            return;
        }
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(modelId, 1);
        List<ModelField> top = topFields(fields);
        String selfSuffix = tableSuffixOf(modelId);

        // 1) 顶层附件 id 收集（含结构体内嵌附件 id，统一一次批量取 URL）
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.addAll(collectAttachmentIds(fields, row));
        }
        Map<Long, String> urlMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        // Q1 多尺寸：id → sizes JSON
        Map<Long, String> sizesMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> img : modelDataDao.findImageUrls(new ArrayList<>(ids))) {
                long id = ((Number) img.get("id")).longValue();
                Object url = img.get("imgUrl");
                urlMap.put(id, url == null ? "" : String.valueOf(url));
                Object name = img.get("imgName");
                nameMap.put(id, name == null ? "" : String.valueOf(name));
                // Q1 多尺寸：id → sizes JSON（供 {field}Srcset 响应式输出）
                Object sz = img.get("sizes");
                sizesMap.put(id, sz == null ? "" : String.valueOf(sz));
            }
        }
        for (Map<String, Object> row : rows) {
            for (ModelField f : top) {
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
                    if (type == FieldTypeEnum.IMAGE) {
                        putImageSrcset(row, f.getFieldName(), id, sizesMap);
                    }
                } else {
                    List<String> urls = new ArrayList<>();
                    List<String> srcsets = new ArrayList<>();
                    for (String s : parseStringArray(String.valueOf(v))) {
                        try {
                            long iid = Long.parseLong(s);
                            urls.add(urlMap.getOrDefault(iid, ""));
                            // S2：多图逐图 Srcset（与 Urls 下标对齐，模板 ${g.gallerySrcsets[i]}）
                            Map<String, Object> sub = new HashMap<>();
                            putImageSrcset(sub, "s", iid, sizesMap);
                            srcsets.add(String.valueOf(sub.getOrDefault("sSrcset", "")));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    row.put(f.getFieldName() + "Urls", urls);
                    row.put(f.getFieldName() + "Srcsets", srcsets);
                }
            }
        }
        // 2) P1 结构体内嵌附件展开：JSON 解析后注入 {子字段}Url/{子字段}Urls，值替换为结构化对象
        expandStructureAttachments(top, childrenOf(fields), rows, urlMap);
        // 3) E1 关联展开（P2：按 lookupFields 追加目标行展示列）
        expandReferences(selfSuffix, top, rows);
        // 4) P2 Rollup 聚合
        expandRollup(selfSuffix, top, rows);
        // 5) P2 M2A 任意关系展开
        expandM2a(model.getCode(), top, rows);
        // 6) P2 双向标注：被哪些模型的内容引用着
        expandBackRefs(model, rows);
        // 7) B1 绑定数据源展开：user → {field}Obj={userId,nickName,avatar,shortUrl}；
        //    category → {field}Obj={id,name,fatherId}（绑定模型 code 见 relate_model，留空=本模型）
        expandBoundEntities(model, top, rows);
        // 8) E6 公式字段：行内表达式受限求值（不落库，仅读侧）
        expandFormulas(top, rows);
        // 9) W 时间范围展开：date_range/datetime_range → {field}Start/{field}End（模板直出起止）
        for (Map<String, Object> row : rows) {
            for (ModelField f : top) {
                FieldTypeEnum type;
                try {
                    type = FieldTypeEnum.of(f.getFieldType());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (type != FieldTypeEnum.DATE_RANGE && type != FieldTypeEnum.DATETIME_RANGE) {
                    continue;
                }
                Object v = row.get(f.getFieldName());
                if (v == null) {
                    continue;
                }
                List<String> arr = parseStringArray(String.valueOf(v));
                row.put(f.getFieldName() + "Start", arr.size() > 0 ? arr.get(0) : null);
                row.put(f.getFieldName() + "End", arr.size() > 1 ? arr.get(1) : null);
            }
        }
    }

    /**
     * E6 公式求值：表达式已在校验期限定字符集（字段名/数字/四则/括号），
     * 以行 Map 为 SpEL 根对象求值（SimpleEvaluationContext 禁类型引用/方法调用）。
     * 求值失败安全降级为 null，不影响行内其他数据。
     */
    private void expandFormulas(List<ModelField> top, List<Map<String, Object>> rows) {
        List<ModelField> formulaFields = new ArrayList<>();
        for (ModelField f : top) {
            if (isType(f, FieldTypeEnum.FORMULA)) {
                formulaFields.add(f);
            }
        }
        if (formulaFields.isEmpty()) {
            return;
        }
        var parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
        var cache = new HashMap<String, org.springframework.expression.Expression>();
        for (Map<String, Object> row : rows) {
            for (ModelField f : formulaFields) {
                String formula = StringUtils.trimToEmpty(f.getFormula());
                if (formula.isEmpty()) {
                    continue;
                }
                try {
                    // Map root 上裸属性名解析不到（SimpleEvaluationContext 无 MapAccessor），
                    // 统一转成 #root['字段'] 索引形式（标识符已在校验期白名单化）
                    String spel = formula.replaceAll("([a-z_][a-z0-9_]*)", "#root['$1']");
                    var expr = cache.computeIfAbsent(spel, k -> parser.parseRaw(k));
                    var ctx = org.springframework.expression.spel.support.SimpleEvaluationContext
                            .forReadOnlyDataBinding().withRootObject(row).build();
                    Object v = expr.getValue(ctx);
                    row.put(f.getFieldName(), v == null ? null : String.valueOf(v));
                } catch (Exception e) {
                    log.warn("公式求值失败（{}#{}）：{}", f.getFieldName(), formula, e.getMessage());
                }
            }
        }
    }

    /**
     * B1 绑定实体读侧展开（对标 ACF User/Taxonomy 字段读回）：与 E1 关联展开同构——
     * 一次批量查询，悬空引用（用户/分类已删）安全跳过，模板侧 {@code ??} 判空降级。
     */
    private void expandBoundEntities(Model model, List<ModelField> top, List<Map<String, Object>> rows) {
        // ---- USER 字段：一次批量取用户 ----
        java.util.Set<Long> userIds = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            for (ModelField f : top) {
                if (isType(f, FieldTypeEnum.USER)) {
                    Long uid = asLong(row.get(f.getFieldName()));
                    if (uid != null) {
                        userIds.add(uid);
                    }
                }
            }
        }
        Map<Long, com.flycms.module.user.model.User> userById = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (com.flycms.module.user.model.User u : userService.getUsersByIds(new ArrayList<>(userIds))) {
                userById.put(u.getUserId(), u);
            }
        }
        // ---- CATEGORY 字段：按绑定模型整树取回（分类量级小，内存建索引） ----
        Map<String, Map<Long, com.flycms.module.model.model.ModelCategory>> catIndexByCode = new HashMap<>();
        for (ModelField f : top) {
            if (!isType(f, FieldTypeEnum.CATEGORY)) {
                continue;
            }
            String boundCode = StringUtils.defaultIfBlank(f.getRelateModel(),
                    model == null ? null : model.getCode());
            if (boundCode == null || catIndexByCode.containsKey(boundCode)) {
                continue;
            }
            com.flycms.module.model.model.Model bound = modelService.findModelByCode(boundCode);
            Map<Long, com.flycms.module.model.model.ModelCategory> idx = new HashMap<>();
            if (bound != null) {
                for (com.flycms.module.model.model.ModelCategory c
                        : modelCategoryService.findCategoriesByModelId(bound.getId(), null)) {
                    idx.put(c.getId(), c);
                }
            }
            catIndexByCode.put(boundCode, idx);
        }
        // ---- 回填 {field}Obj ----
        for (Map<String, Object> row : rows) {
            for (ModelField f : top) {
                if (isType(f, FieldTypeEnum.USER)) {
                    Long uid = asLong(row.get(f.getFieldName()));
                    com.flycms.module.user.model.User u = uid == null ? null : userById.get(uid);
                    if (u != null) {
                        Map<String, Object> obj = new LinkedHashMap<>();
                        obj.put("userId", String.valueOf(u.getUserId()));
                        // 昵称缺失回退用户名（与前台展示口径一致，模板永不输出 null）
                        String nick = StringUtils.defaultIfBlank(u.getNickName(), u.getUserName());
                        obj.put("nickName", nick);
                        obj.put("userName", u.getUserName());
                        obj.put("avatar", u.getAvatar());
                        obj.put("shortUrl", u.getShortUrl());
                        row.put(f.getFieldName() + "Obj", obj);
                    }
                } else if (isType(f, FieldTypeEnum.CATEGORY)) {
                    Long cid = asLong(row.get(f.getFieldName()));
                    String boundCode = StringUtils.defaultIfBlank(f.getRelateModel(),
                            model == null ? null : model.getCode());
                    Map<Long, com.flycms.module.model.model.ModelCategory> idx =
                            boundCode == null ? Map.of()
                                              : catIndexByCode.getOrDefault(boundCode, Map.of());
                    com.flycms.module.model.model.ModelCategory c = cid == null ? null : idx.get(cid);
                    if (c != null) {
                        Map<String, Object> obj = new LinkedHashMap<>();
                        obj.put("id", String.valueOf(c.getId()));
                        obj.put("name", c.getName());
                        obj.put("fatherId", String.valueOf(c.getFatherId()));
                        row.put(f.getFieldName() + "Obj", obj);
                    }
                }
            }
        }
    }

    private boolean isType(ModelField f, FieldTypeEnum expected) {
        try {
            return FieldTypeEnum.of(f.getFieldType()) == expected;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** E8：宽松解析 yyyy-MM-dd / yyyy-MM-dd HH:mm:ss，非法返回 null */
    private String parseTime(String v) {
        String t = StringUtils.trimToEmpty(v).replace('T', ' ');
        return (t.matches("^\\d{4}-\\d{2}-\\d{2}$") || t.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?$"))
                ? t : null;
    }

    private Long asLong(Object v) {
        if (v == null || StringUtils.isBlank(String.valueOf(v))) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * P1 结构字段内嵌附件展开：把 json 列的字符串值解析为 Map（GROUP）/ List&lt;Map&gt;（REPEATER），
     * 附件子字段注入 {@code {子字段名}Url} / {@code {子字段名}Urls} 后整体替换原值。
     */
    private void expandStructureAttachments(List<ModelField> top, Map<Long, List<ModelField>> children,
                                            List<Map<String, Object>> rows, Map<Long, String> urlMap) {
        for (ModelField f : top) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!type.isStructure()) {
                continue;
            }
            List<ModelField> kids = children.getOrDefault(f.getId(), List.of());
            List<ModelField> attKids = new ArrayList<>();
            for (ModelField k : kids) {
                try {
                    if (FieldTypeEnum.of(k.getFieldType()).isAttachment()) {
                        attKids.add(k);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            // 无子字段时保持原值；否则一律解析为结构化对象/数组（附件子字段顺带注入 URL 别名）
            if (kids.isEmpty()) {
                continue;
            }
            for (Map<String, Object> row : rows) {
                Object v = row.get(f.getFieldName());
                if (v == null || !(v instanceof String s) || s.isBlank()) {
                    continue;
                }
                try {
                    if (type == FieldTypeEnum.GROUP) {
                        Map<String, Object> obj = JSON.parseObject(s, Map.class);
                        if (obj == null) {
                            continue;
                        }
                        enrichStructureRow(obj, attKids, urlMap);
                        row.put(f.getFieldName(), obj);
                    } else {
                        List<Object> arr = JSON.parseArray(s);
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (Object o : arr == null ? List.of() : arr) {
                            if (o instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = (Map<String, Object>) o;
                                enrichStructureRow(m, attKids, urlMap);
                                list.add(m);
                            }
                        }
                        row.put(f.getFieldName(), list);
                    }
                } catch (Exception e) {
                    log.warn("结构字段[{}]附件展开失败（保持原值）：{}", f.getFieldName(), e.getMessage());
                }
            }
        }
    }

    /** 单个结构行内附件子字段注入 URL 别名 */
    private void enrichStructureRow(Map<String, Object> obj, List<ModelField> attKids, Map<Long, String> urlMap) {
        for (ModelField k : attKids) {
            Object cv = obj.get(k.getFieldName());
            if (cv == null) {
                continue;
            }
            FieldTypeEnum kt = FieldTypeEnum.of(k.getFieldType());
            try {
                if (kt == FieldTypeEnum.IMAGE || kt == FieldTypeEnum.FILE) {
                    long id = Long.parseLong(String.valueOf(cv));
                    obj.put(k.getFieldName() + "Url", urlMap.getOrDefault(id, ""));
                } else {
                    List<String> urls = new ArrayList<>();
                    for (String item : parseStringArray(String.valueOf(cv))) {
                        try {
                            urls.add(urlMap.getOrDefault(Long.parseLong(item.trim()), ""));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    obj.put(k.getFieldName() + "Urls", urls);
                }
            } catch (NumberFormatException ignored) {
            }
        }
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
            // 第二遍：一次批量取回目标行（P2：按 lookupFields 追加展示列）
            List<Map<String, Object>> targets =
                    modelDataDao.findRowsByIds(target, new ArrayList<>(all), lookupColumns(f));
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

    /**
     * P2 Lookup：解析字段的 lookupFields（JSON 字符串数组），逐列过列名白名单后作为
     * 关联展开（{field}Obj/{field}List）的追加展示列，上限 10 列。
     */
    private List<String> lookupColumns(ModelField f) {
        if (StringUtils.isBlank(f.getLookupFields())) {
            return null;
        }
        List<String> cols = new ArrayList<>();
        try {
            List<String> arr = JSON.parseArray(f.getLookupFields(), String.class);
            for (String c : arr == null ? List.<String>of() : arr) {
                if (cols.size() >= 10) {
                    break;
                }
                try {
                    String safe = SqlSafeUtil.safeColumnName(c);
                    if (!cols.contains(safe)) {
                        cols.add(safe);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return cols.isEmpty() ? null : cols;
    }

    /**
     * P2 Rollup 聚合展开（NocoDB 语义）：对 rollup 字段按其 source（本模型 RELATE 字段）
     * 指向的目标行做 COUNT/SUM/AVG/MIN/MAX，结果写入 {@code row[fieldName]}。
     * 一次批量取回全部被引用目标行，按行在 Java 侧聚合。
     */
    private void expandRollup(String selfSuffix, List<ModelField> top, List<Map<String, Object>> rows) {
        for (ModelField f : top) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (type != FieldTypeEnum.ROLLUP || StringUtils.isBlank(f.getRollupExpr())) {
                continue;
            }
            Map<String, Object> expr;
            try {
                expr = JSON.parseObject(f.getRollupExpr());
            } catch (Exception e) {
                continue;
            }
            if (expr == null) {
                continue;
            }
            String source = String.valueOf(expr.get("source"));
            String func = String.valueOf(expr.get("func"));
            String column = expr.get("column") == null ? null : String.valueOf(expr.get("column"));
            if (!ROLLUP_FUNCS.contains(func)) {
                continue;
            }
            ModelField src = null;
            for (ModelField t : top) {
                if (t.getFieldName().equals(source)) {
                    src = t;
                    break;
                }
            }
            if (src == null) {
                continue;
            }
            // 第一遍：按行解析出引用的目标 id
            Map<Map<String, Object>, List<Long>> perRow = new java.util.IdentityHashMap<>();
            java.util.Set<Long> all = new java.util.LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                List<Long> linked = readReferenceIds(FieldTypeEnum.RELATE, src, row);
                if (linked.isEmpty()) {
                    continue;
                }
                perRow.put(row, linked);
                all.addAll(linked);
            }
            if (all.isEmpty()) {
                continue;
            }
            String targetSuffix;
            try {
                targetSuffix = StringUtils.isBlank(src.getRelateModel())
                        ? selfSuffix : SqlSafeUtil.safeTableSuffixForExisting(src.getRelateModel());
            } catch (IllegalArgumentException e) {
                continue;
            }
            try {
                List<Map<String, Object>> targets = modelDataDao.rollupAggregate(
                        targetSuffix, source, func, "COUNT".equals(func) ? null : column, new ArrayList<>(all));
                Map<Long, Object> valById = new HashMap<>();
                for (Map<String, Object> t : targets) {
                    Object refId = t.get("refId");
                    if (refId != null) {
                        valById.put(Long.parseLong(String.valueOf(refId)), t.get("aggVal"));
                    }
                }
                for (Map.Entry<Map<String, Object>, List<Long>> e : perRow.entrySet()) {
                    e.getKey().put(f.getFieldName(),
                            aggregate(func, e.getValue(), valById));
                }
            } catch (Exception e) {
                log.warn("rollup 字段[{}]聚合失败：{}", f.getFieldName(), e.getMessage());
            }
        }
    }

    /** 按白名单函数对一组引用目标行的取值做聚合（缺失目标行跳过；COUNT 按行存在数计） */
    private Object aggregate(String func, List<Long> ids, Map<Long, Object> valById) {
        List<Double> nums = new ArrayList<>();
        int count = 0;
        for (Long id : ids) {
            if (!valById.containsKey(id)) {
                continue; // 目标行不存在或未发布
            }
            count++;
            Object val = valById.get(id);
            if (!"COUNT".equals(func) && val != null) {
                try {
                    nums.add(Double.parseDouble(String.valueOf(val)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        switch (func) {
            case "COUNT":
                return count;
            case "SUM":
                return nums.stream().mapToDouble(Double::doubleValue).sum();
            case "AVG":
                return nums.isEmpty() ? null
                        : nums.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            case "MIN":
                return nums.isEmpty() ? null : nums.stream().min(Double::compare).orElse(null);
            case "MAX":
                return nums.isEmpty() ? null : nums.stream().max(Double::compare).orElse(null);
            default:
                return null;
        }
    }

    /**
     * P2 M2A 展开：读 fly_relation，按目标模型分组批量取行，
     * 写入 {@code row[fieldName + "List"]} = [{model:…, id, title, …}]（保持 sort 顺序）。
     */
    private void expandM2a(String fromModel, List<ModelField> top, List<Map<String, Object>> rows) {
        boolean hasM2a = false;
        for (ModelField f : top) {
            try {
                if (FieldTypeEnum.of(f.getFieldType()) == FieldTypeEnum.M2A) {
                    hasM2a = true;
                    break;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!hasM2a) {
            return;
        }
        List<Long> rowIds = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id != null) {
                try {
                    rowIds.add(Long.parseLong(String.valueOf(id)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (rowIds.isEmpty()) {
            return;
        }
        for (ModelField f : top) {
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (type != FieldTypeEnum.M2A) {
                continue;
            }
            List<Map<String, Object>> rels;
            try {
                rels = modelDataDao.findRelations(fromModel, rowIds, f.getFieldName());
            } catch (Exception e) {
                log.warn("m2a 字段[{}]关系读取失败：{}", f.getFieldName(), e.getMessage());
                continue;
            }
            if (rels == null || rels.isEmpty()) {
                continue;
            }
            // 目标行批量取回：按 toModel 分组
            Map<String, java.util.Set<Long>> idsByModel = new java.util.LinkedHashMap<>();
            for (Map<String, Object> r : rels) {
                idsByModel.computeIfAbsent(String.valueOf(r.get("toModel")), k -> new java.util.LinkedHashSet<>())
                        .add(Long.parseLong(String.valueOf(r.get("toId"))));
            }
            Map<String, Map<Long, Map<String, Object>>> rowsByModel = new HashMap<>();
            for (Map.Entry<String, java.util.Set<Long>> e : idsByModel.entrySet()) {
                try {
                    List<Map<String, Object>> targets = modelDataDao.findRowsByIds(
                            SqlSafeUtil.safeTableSuffixForExisting(e.getKey()), new ArrayList<>(e.getValue()), null);
                    normalizeKeys(targets);
                    Map<Long, Map<String, Object>> byId = new HashMap<>();
                    for (Map<String, Object> t : targets) {
                        Object tid = t.get("id");
                        if (tid != null) {
                            byId.put(Long.parseLong(String.valueOf(tid)), t);
                        }
                    }
                    rowsByModel.put(e.getKey(), byId);
                } catch (IllegalArgumentException ex) {
                    log.warn("m2a 字段[{}]目标模型标识非法：{}", f.getFieldName(), e.getKey());
                }
            }
            Map<Long, List<Map<String, Object>>> byFrom = new HashMap<>();
            for (Map<String, Object> r : rels) {
                long fromId = Long.parseLong(String.valueOf(r.get("fromId")));
                Map<Long, Map<String, Object>> byId = rowsByModel.get(String.valueOf(r.get("toModel")));
                if (byId == null) {
                    continue;
                }
                Map<String, Object> target = byId.get(Long.parseLong(String.valueOf(r.get("toId"))));
                if (target == null) {
                    continue; // 悬空引用静默跳过
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("model", r.get("toModel"));
                item.putAll(target);
                byFrom.computeIfAbsent(fromId, k -> new ArrayList<>()).add(item);
            }
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                if (id == null) {
                    continue;
                }
                try {
                    List<Map<String, Object>> list = byFrom.get(Long.parseLong(String.valueOf(id)));
                    if (list != null) {
                        row.put(f.getFieldName() + "List", list);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * P2 双向标注：查「哪些模型的单值 relate 字段指向本模型」，为每行注入
     * {@code row[fieldName + "Backs"]} = [{id, shortUrl, title}]（来源行，仅已发布）。
     * 无物理列、无需目标模型预定义反向字段。
     */
    /**
     * Q3 内容图片本地化：模型开启 localize_images=1 时，content 里的外站 <img>
     * 抓取到本地（登记 fly_images）并把 src 替换为本地地址；域名取站点参数
     * fly_img_domain（空=相对路径）。失败静默降级为原 content（不阻塞保存）。
     */
    private String localizeContentIfEnabled(Model model, String content, Long userId) {
        if (content == null || content.isBlank()
                || model.getLocalizeImages() == null || model.getLocalizeImages() != 1) {
            return content;
        }
        try {
            String domain = configService.getStringByKey("fly_img_domain");
            return imagesService.localizeContent(userId, content, domain);
        } catch (Exception e) {
            log.warn("内容图片本地化失败（model={}）：{}", model.getCode(), e.getMessage());
            return content;
        }
    }

    /**
     * Q1 媒体库多尺寸：单图 {field}Srcset 生成（sizes JSON → "u 150w, u 320w, …" 按宽升序）。
     * sizes 缺失/损坏静默跳过（读侧回退原图），不阻塞行展开。
     */
    private void putImageSrcset(Map<String, Object> row, String fieldName, Long id,
                                Map<Long, String> sizesMap) {
        String szJson = sizesMap.getOrDefault(id, "");
        if (StringUtils.isBlank(szJson)) {
            return;
        }
        try {
            List<String> parts = new ArrayList<>();
            for (Object o : JSON.parseArray(szJson)) {
                if (o instanceof Map) {
                    Object w = ((Map<?, ?>) o).get("w");
                    Object u = ((Map<?, ?>) o).get("u");
                    if (w != null && u != null) {
                        parts.add(u + " " + w + "w");
                    }
                }
            }
            if (!parts.isEmpty()) {
                row.put(fieldName + "Srcset", String.join(", ", parts));
            }
        } catch (Exception ignored) {
            // sizes 损坏按无副本处理
        }
    }

    private void expandBackRefs(Model self, List<Map<String, Object>> rows) {
        List<ModelField> incoming;
        try {
            incoming = modelFieldDao.findRelateFieldsByTargetModel(self.getCode());
        } catch (Exception e) {
            return;
        }
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id != null) {
                try {
                    ids.add(Long.parseLong(String.valueOf(id)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        for (ModelField f : incoming) {
            Model srcModel = modelService.findModelById(f.getModelId());
            if (srcModel == null || StringUtils.isBlank(srcModel.getCode())) {
                continue;
            }
            String srcSuffix;
            try {
                srcSuffix = SqlSafeUtil.safeTableSuffixForExisting(srcModel.getCode());
            } catch (IllegalArgumentException e) {
                continue;
            }
            List<Map<String, Object>> refs;
            try {
                refs = modelDataDao.findReverseRefs(srcSuffix, f.getFieldName(), ids);
            } catch (Exception e) {
                log.warn("反向引用查询失败：model={}, field={}, err={}", srcModel.getCode(), f.getFieldName(), e.getMessage());
                continue;
            }
            if (refs == null || refs.isEmpty()) {
                continue;
            }
            normalizeKeys(refs);
            Map<Long, List<Map<String, Object>>> byRef = new HashMap<>();
            for (Map<String, Object> r : refs) {
                Object refId = r.get("refId");
                if (refId == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", r.get("id"));
                item.put("shortUrl", r.get("shortUrl"));
                item.put("title", r.get("title"));
                try {
                    byRef.computeIfAbsent(Long.parseLong(String.valueOf(refId)), k -> new ArrayList<>()).add(item);
                } catch (NumberFormatException ignored) {
                }
            }
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                if (id == null) {
                    continue;
                }
                try {
                    List<Map<String, Object>> list = byRef.get(Long.parseLong(String.valueOf(id)));
                    if (list != null) {
                        row.put(f.getFieldName() + "Backs", list);
                    }
                } catch (NumberFormatException ignored) {
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
     * 遍历顶层字段定义校验 form 值并写入 values/columns（列名过白名单）。
     * refIds 收集附件引用（fly_images.id，含 JSON 数组与结构体内 id）；
     * m2aOut 收集 M2A 关系待写列表（值 = [{model,id}]，内容行落库后统一同步 fly_relation）。
     *
     * <p>P1 结构层语义：
     * <ul>
     *   <li>只遍历顶层字段（parentId=0），GROUP/REPEATER 按子字段 schema 递归校验后整包 JSON 落列；</li>
     *   <li>visible_when 命中「隐藏」的字段跳过校验与写列（必填也不生效）。</li>
     * </ul>
     *
     * @param selfSuffix 本模型的物理表后缀，供 RELATE 自关联（relate_model 留空）解析目标表
     */
    private DataVo buildDynamicValues(List<ModelField> allFields, Map<String, String> form,
                                      Map<String, Object> values, List<String> columns, List<Long> refIds,
                                      Map<String, List<Map<String, Object>>> m2aOut, String selfSuffix) {
        List<ModelField> fields = topFields(allFields);
        Map<Long, List<ModelField>> children = childrenOf(allFields);
        for (ModelField f : fields) {
            String raw = form.get(f.getFieldName());
            boolean blank = StringUtils.isBlank(raw);
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                return DataVo.failure("字段[" + f.getFieldName() + "]类型非法");
            }
            // P2 虚拟字段：rollup 读取时聚合不落列；m2a 落 fly_relation 中间表（此处只校验）
            if (type.isVirtual()) {
                if (type == FieldTypeEnum.M2A && !blank) {
                    DataVo r = validateM2a(f, raw, m2aOut);
                    if (r.getCode() != DataVo.CODE_SUCCESS) {
                        return r;
                    }
                }
                continue;
            }
            if (blank && StringUtils.isNotBlank(f.getDefaultValue())) {
                raw = f.getDefaultValue();
                blank = false;
            }
            // P1 条件显隐：隐藏字段跳过校验与写列（默认值也不落库）
            if (!isVisible(f, form)) {
                continue;
            }
            // 表单隐藏（is_form=0）：不在内容表单出现/提交的字段跳过校验与写列，
            // 语义同条件显隐未命中——必填、默认值均失效；字段仍参与列表/详情/模板。
            if (f.getIsForm() == 0) {
                continue;
            }
            if (blank) {
                if (f.getIsRequired() == 1) {
                    return DataVo.failure(f.getFieldLabel() + "不能为空");
                }
                continue;
            }
            Object v;
            try {
                if (type.isStructure()) {
                    DataVo sv = buildStructureValue(f, children.getOrDefault(f.getId(), List.of()),
                            raw, refIds, selfSuffix);
                    if (sv.getCode() != DataVo.CODE_SUCCESS) {
                        return sv;
                    }
                    // json 列必须存 JSON 字符串（Map/List 直传会被驱动按二进制序列化，MySQL 拒绝）
                    v = JSON.toJSONString(sv.getData());
                } else {
                    v = convertFieldValue(f, type, raw, refIds, selfSuffix);
                }
            } catch (IllegalArgumentException e) {
                return DataVo.failure(StringUtils.defaultIfBlank(e.getMessage(), f.getFieldLabel() + "格式不正确"));
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

    /** 顶层字段（P1：parentId 为空或 0） */
    private static List<ModelField> topFields(List<ModelField> fields) {
        List<ModelField> top = new ArrayList<>();
        for (ModelField f : fields) {
            if (f.getParentId() == null || f.getParentId() == 0) {
                top.add(f);
            }
        }
        return top;
    }

    /** 子字段索引：parent 字段 id → 子字段列表（P1） */
    private static Map<Long, List<ModelField>> childrenOf(List<ModelField> fields) {
        Map<Long, List<ModelField>> map = new HashMap<>();
        for (ModelField f : fields) {
            if (f.getParentId() != null && f.getParentId() > 0) {
                map.computeIfAbsent(f.getParentId(), k -> new ArrayList<>()).add(f);
            }
        }
        return map;
    }

    /**
     * P1 条件显隐求值。visible_when = {"field":"x","op":"eq|neq|in|notin|empty|notempty","value":"1"}。
     * 返回 true = 显示（参与校验）；表达式缺失或解析失败一律按显示处理（保存时已做结构校验，防御性兜底）。
     */
    private boolean isVisible(ModelField f, Map<String, String> form) {
        if (StringUtils.isBlank(f.getVisibleWhen())) {
            return true;
        }
        try {
            Map<String, Object> cond = JSON.parseObject(f.getVisibleWhen());
            if (cond == null) {
                return true;
            }
            String ref = String.valueOf(cond.get("field"));
            String op = String.valueOf(cond.get("op"));
            String expected = cond.get("value") == null ? "" : String.valueOf(cond.get("value"));
            String actual = form.get(ref);
            boolean present = StringUtils.isNotBlank(actual);
            String actualTrim = present ? actual.trim() : "";
            switch (op) {
                case "eq":
                    return present && actualTrim.equals(expected);
                case "neq":
                    return !present || !actualTrim.equals(expected);
                case "in":
                    return present && Arrays.asList(expected.split(",")).contains(actualTrim);
                case "notin":
                    return !present || !Arrays.asList(expected.split(",")).contains(actualTrim);
                case "empty":
                    return !present;
                case "notempty":
                    return present;
                default:
                    return true;
            }
        } catch (Exception e) {
            log.warn("visible_when 解析失败，按显示处理：field={}, expr={}", f.getFieldName(), f.getVisibleWhen());
            return true;
        }
    }

    /**
     * P1 结构字段（GROUP 对象 / REPEATER 数组）值构建：按子字段 schema 逐个递归校验转换，
     * 返回序列化前的 Map / List&lt;Map&gt;（由调用方 JSON.toJSONString 落 json 列）。
     */
    @SuppressWarnings("unchecked")
    private DataVo buildStructureValue(ModelField parent, List<ModelField> children, String raw,
                                       List<Long> refIds, String selfSuffix) {
        boolean multiple = FieldTypeEnum.of(parent.getFieldType()) == FieldTypeEnum.REPEATER;
        if (children.isEmpty()) {
            return DataVo.success("ok", multiple ? new ArrayList<>() : new LinkedHashMap<String, Object>());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            if (multiple) {
                List<Object> arr = JSON.parseArray(raw);
                if (arr != null) {
                    for (Object o : arr) {
                        if (!(o instanceof Map)) {
                            return DataVo.failure(parent.getFieldLabel() + "的每一行必须是对象");
                        }
                        rows.add((Map<String, Object>) o);
                    }
                }
            } else {
                Map<String, Object> obj = JSON.parseObject(raw, Map.class);
                if (obj != null) {
                    rows.add(obj);
                }
            }
        } catch (Exception e) {
            return DataVo.failure(parent.getFieldLabel() + "必须是合法 JSON" + (multiple ? "数组" : "对象"));
        }
        if (multiple) {
            if (rows.size() > 50) {
                return DataVo.failure(parent.getFieldLabel() + "最多 50 行");
            }
            if (rows.isEmpty() && parent.getIsRequired() == 1) {
                return DataVo.failure(parent.getFieldLabel() + "至少需要一行");
            }
        }
        List<Object> outArr = multiple ? new ArrayList<>() : null;
        Map<String, Object> outObj = multiple ? null : new LinkedHashMap<>();
        for (Map<String, Object> rowMap : rows) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (ModelField child : children) {
                Object cv = rowMap.get(child.getFieldName());
                String childRaw = cv == null ? null
                        : (cv instanceof String s ? s : String.valueOf(cv));
                FieldTypeEnum ct;
                try {
                    ct = FieldTypeEnum.of(child.getFieldType());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (StringUtils.isBlank(childRaw)) {
                    if (child.getIsRequired() == 1) {
                        return DataVo.failure(
                                parent.getFieldLabel() + "的「" + child.getFieldLabel() + "」不能为空");
                    }
                    continue;
                }
                Object v2;
                try {
                    v2 = convertFieldValue(child, ct, childRaw, refIds, selfSuffix);
                } catch (IllegalArgumentException e) {
                    return DataVo.failure(parent.getFieldLabel() + "的「" + child.getFieldLabel() + "」："
                            + StringUtils.defaultIfBlank(e.getMessage(), "格式不正确"));
                } catch (Exception e) {
                    log.error("结构子字段[{}.{}]({})处理失败: {}", parent.getFieldName(), child.getFieldName(),
                            child.getFieldType(), e.getMessage(), e);
                    return DataVo.failure(parent.getFieldLabel() + "的「" + child.getFieldLabel() + "」处理失败");
                }
                clean.put(child.getFieldName(), v2);
            }
            if (multiple) {
                outArr.add(clean);
            } else {
                outObj = clean;
            }
        }
        return DataVo.success("ok", multiple ? outArr : outObj);
    }

    /**
     * 单字段值转换与校验（顶层与结构子字段共用）。只做「字符串 → 落库值」的定形，
     * 空值语义 / 可见性 / 列写入由调用方处理；校验失败抛 IllegalArgumentException（message 为用户可读原因）。
     * 数值区间（min/max，P0）在本方法尾部统一执行。
     */
    private Object convertFieldValue(ModelField f, FieldTypeEnum type, String raw,
                                     List<Long> refIds, String selfSuffix) throws Exception {
        Object v;
        switch (type) {
            case NUMBER:
                v = Long.parseLong(raw.trim());
                break;
            case DECIMAL:
                v = new java.math.BigDecimal(raw.trim());
                break;
            case SWITCH: {
                String s = raw.trim().toLowerCase();
                v = ("1".equals(s) || "true".equals(s) || "on".equals(s)) ? 1 : 0;
                break;
            }
            case EMAIL:
                if (!raw.trim().matches("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "格式不正确");
                }
                v = raw.trim();
                break;
            case URL:
                if (!raw.trim().matches("^https?://\\S+$")) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "必须以 http(s):// 开头");
                }
                v = raw.trim();
                break;
            case PHONE:
                if (!raw.trim().matches("^1[3-9]\\d{9}$")) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "格式不正确");
                }
                v = raw.trim();
                break;
            case COLOR:
                if (!raw.trim().matches("^#[0-9a-fA-F]{3,8}$")) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "必须是 #RGB/#RRGGBB 颜色值");
                }
                v = raw.trim();
                break;
            case RATING:
                v = (int) Long.parseLong(raw.trim());
                break;
            case SLUG: {
                String slug = raw.trim().toLowerCase();
                if (!slug.matches("^[a-z0-9][a-z0-9-]{0,127}$")) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "只允许小写字母、数字和中划线");
                }
                v = slug;
                break;
            }
            case DATE:
                v = new SimpleDateFormat("yyyy-MM-dd").parse(raw.trim());
                break;
            case DATETIME:
                v = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(raw.trim().replace('T', ' '));
                break;
            case DATE_RANGE:
            case DATETIME_RANGE: {
                // W 时间范围：存 ["start","end"]；两段分别校验格式且 start<=end
                List<String> arr = parseStringArray(raw);
                boolean isDate = type == FieldTypeEnum.DATE_RANGE;
                SimpleDateFormat fmt = isDate
                        ? new SimpleDateFormat("yyyy-MM-dd")
                        : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.function.Function<String, String> norm = s -> {
                    String t = s.trim().replace('T', ' ');
                    if (!isDate && t.length() == 16) {
                        t = t + ":00";
                    }
                    try {
                        fmt.parse(t);
                    } catch (java.text.ParseException e) {
                        throw new IllegalArgumentException(
                                f.getFieldLabel() + "时间格式不正确（" + s + "）");
                    }
                    return isDate ? t.substring(0, 10) : t;
                };
                if (arr.size() != 2) {
                    if (arr.isEmpty() && f.getIsRequired() == 1) {
                        throw new IllegalArgumentException(f.getFieldLabel() + "不能为空");
                    }
                    if (arr.isEmpty()) {
                        v = null;
                        break;
                    }
                    throw new IllegalArgumentException(f.getFieldLabel() + "需要起止两个时间");
                }
                String start = norm.apply(arr.get(0));
                String end = norm.apply(arr.get(1));
                if (start.compareTo(end) > 0) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "开始时间不能晚于结束时间");
                }
                v = JSON.toJSONString(Arrays.asList(start, end));
                break;
            }
            case RELATE: {
                // E1 单值关联：目标内容必须存在且 status=1（防悬空引用）
                long rid;
                try {
                    rid = Long.parseLong(raw.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "引用格式不正确");
                }
                DataVo rel = checkReferences(f, selfSuffix, Arrays.asList(rid));
                if (rel.getCode() != DataVo.CODE_SUCCESS) {
                    throw new IllegalArgumentException(rel.getMessage());
                }
                v = rid;
                break;
            }
            case RELATES: {
                // E1 多值关联：存 id 数组（JSON），逐个校验存在且已发布
                List<String> arr = parseStringArray(raw);
                if (arr.isEmpty() && f.getIsRequired() == 1) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "不能为空");
                }
                List<Long> ids = new ArrayList<>();
                for (String a : arr) {
                    try {
                        ids.add(Long.parseLong(a.trim()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(f.getFieldLabel() + "含有非法引用：" + a);
                    }
                }
                if (!ids.isEmpty()) {
                    DataVo rel = checkReferences(f, selfSuffix, distinct(ids));
                    if (rel.getCode() != DataVo.CODE_SUCCESS) {
                        throw new IllegalArgumentException(rel.getMessage());
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
                    throw new IllegalArgumentException(f.getFieldLabel() + "含非法字符（< > \" '）");
                }
                v = url;
                break;
            }
            case CHECKBOX:
            case IMAGES:
            case FILES: {
                List<String> arr = parseStringArray(raw);
                if (arr.isEmpty() && f.getIsRequired() == 1) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "不能为空");
                }
                if (type == FieldTypeEnum.CHECKBOX && f.getOptions() != null) {
                    List<String> opts = parseStringArray(f.getOptions());
                    for (String a : arr) {
                        if (!opts.contains(a)) {
                            throw new IllegalArgumentException(f.getFieldLabel() + "含有非法选项：" + a);
                        }
                    }
                }
                if (type.isAttachment() && !arr.isEmpty()) {
                    List<Long> ids = new ArrayList<>();
                    for (String a : arr) {
                        ids.add(Long.parseLong(a));
                    }
                    if (modelDataDao.countImages(ids) < ids.size()) {
                        throw new IllegalArgumentException(f.getFieldLabel() + "引用的附件不存在");
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
                    throw new IllegalArgumentException(f.getFieldLabel() + "引用的附件不存在");
                }
                refIds.add(aid);
                v = aid;
                break;
            }
            case SELECT:
            case RADIO: {
                if (f.getOptions() != null && !parseStringArray(f.getOptions()).contains(raw)) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "含有非法选项：" + raw);
                }
                v = raw;
                break;
            }
            case USER: {
                // B1 绑定数据源（用户选择器）：必须是真实存在的平台用户——表单显示昵称，落库存 user_id
                long uid;
                try {
                    uid = Long.parseLong(raw.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "格式不正确");
                }
                if (userService.findUserById(uid, 0) == null) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "所选用户不存在");
                }
                v = uid;
                break;
            }
            case CATEGORY: {
                // B1 绑定数据源（分类选择器）：分类必须存在且属于绑定模型的分类树
                // （绑定模型 code 见 relate_model，留空 = 本模型；与主表固定 category_id 通道互补）
                long cid;
                try {
                    cid = Long.parseLong(raw.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "格式不正确");
                }
                com.flycms.module.model.model.Model bound = modelService.findModelByCode(
                        StringUtils.defaultIfBlank(f.getRelateModel(), selfSuffix));
                com.flycms.module.model.model.ModelCategory cat = modelCategoryService.findCategoryById(cid);
                if (cat == null || bound == null || !bound.getId().equals(cat.getModelId())) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "所选分类不存在或不属于绑定模型");
                }
                v = cid;
                break;
            }
            default: {
                // input/textarea/region
                if (f.getMaxlength() != null && raw.length() > f.getMaxlength()) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "长度不能超过" + f.getMaxlength());
                }
                if (type == FieldTypeEnum.INPUT && StringUtils.isNotBlank(f.getRegex())
                        && !raw.matches(f.getRegex())) {
                    throw new IllegalArgumentException(f.getFieldLabel() + "格式不正确");
                }
                v = raw;
            }
        }
        // 数值区间（P0 万能建模批次）：number/decimal/rating 支持 min/max
        if (type == FieldTypeEnum.NUMBER || type == FieldTypeEnum.DECIMAL || type == FieldTypeEnum.RATING) {
            java.math.BigDecimal num = new java.math.BigDecimal(String.valueOf(v));
            if (f.getMinValue() != null && num.compareTo(f.getMinValue()) < 0) {
                throw new IllegalArgumentException(
                        f.getFieldLabel() + "不能小于" + f.getMinValue().stripTrailingZeros().toPlainString());
            }
            if (f.getMaxValue() != null && num.compareTo(f.getMaxValue()) > 0) {
                throw new IllegalArgumentException(
                        f.getFieldLabel() + "不能大于" + f.getMaxValue().stripTrailingZeros().toPlainString());
            }
        }
        return v;
    }

    /**
     * P2 M2A 值校验：form 值 = [{"model":"目标模型code","id":目标内容id},…]，
     * 逐项校验目标模型标识与内容存在性（status=1），通过后写入 m2aOut 待同步。
     */
    @SuppressWarnings("unchecked")
    private DataVo validateM2a(ModelField f, String raw, Map<String, List<Map<String, Object>>> m2aOut) {
        List<Map<String, Object>> arr;
        try {
            List<Object> parsed = JSON.parseArray(raw);
            arr = new ArrayList<>();
            for (Object o : parsed == null ? List.of() : parsed) {
                if (!(o instanceof Map)) {
                    return DataVo.failure(f.getFieldLabel() + "必须是 [{model,id}] 数组");
                }
                arr.add((Map<String, Object>) o);
            }
        } catch (Exception e) {
            return DataVo.failure(f.getFieldLabel() + "必须是 [{model,id}] 数组");
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Map<String, Object> item : arr == null ? List.<Map<String, Object>>of() : arr) {
            String toModel = item.get("model") == null ? "" : String.valueOf(item.get("model")).trim();
            long toId;
            try {
                toId = Long.parseLong(String.valueOf(item.get("id")).trim());
            } catch (Exception e) {
                return DataVo.failure(f.getFieldLabel() + "引用格式不正确");
            }
            if (toModel.isEmpty()) {
                return DataVo.failure(f.getFieldLabel() + "缺少目标模型");
            }
            String target;
            try {
                target = SqlSafeUtil.safeTableSuffixForExisting(toModel);
            } catch (IllegalArgumentException e) {
                return DataVo.failure(f.getFieldLabel() + "的目标模型标识非法：" + toModel);
            }
            List<Map<String, Object>> rows = modelDataDao.findRowsByIds(target, Arrays.asList(toId), null);
            if (rows.isEmpty() || toInt(rows.get(0).get("status")) != 1) {
                return DataVo.failure(f.getFieldLabel() + "引用的内容不存在或未发布");
            }
            if (seen.add(toModel + ":" + toId)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("toModel", toModel);
                entry.put("toId", toId);
                entries.add(entry);
            }
        }
        m2aOut.put(f.getFieldName(), entries);
        return DataVo.success("ok");
    }

    /**
     * P2 M2A 关系同步（覆盖式）：删旧插新。m2aOut 为空表示本次未提交任何 m2a 字段，不触碰既有关系。
     */
    private void syncRelations(Model model, Long id, Map<String, List<Map<String, Object>>> m2aOut) {
        if (m2aOut.isEmpty() || model == null || StringUtils.isBlank(model.getCode())) {
            return;
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : m2aOut.entrySet()) {
            modelDataDao.deleteRelations(model.getCode(), Arrays.asList(id), e.getKey());
            List<Map<String, Object>> rows = e.getValue();
            if (rows.isEmpty()) {
                continue;
            }
            List<Map<String, Object>> inserts = new ArrayList<>();
            SnowFlake snowFlake = SnowFlake.getInstance();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> r = new HashMap<>();
                r.put("id", snowFlake.nextId());
                r.put("fromModel", model.getCode());
                r.put("fromId", id);
                r.put("fieldName", e.getKey());
                r.put("toModel", rows.get(i).get("toModel"));
                r.put("toId", rows.get(i).get("toId"));
                r.put("sort", i);
                inserts.add(r);
            }
            modelDataDao.insertRelations(inserts);
        }
    }

    /**
     * 唯一约束（P0 万能建模批次）：is_unique=1 的字段值在全模型内不得重复。
     * 在 buildDynamicValues 之后调用（列名过白名单、值已定形）；excludeId 用于更新场景排除自身。
     */
    private DataVo checkUnique(List<ModelField> fields, Map<String, Object> values,
                               String suffix, Long excludeId) {
        for (ModelField f : fields) {
            if (f.getIsUnique() == null || f.getIsUnique() != 1) {
                continue;
            }
            Object v = values.get(f.getFieldName());
            if (v == null || "".equals(v)) {
                continue;
            }
            int dup = modelDataDao.countDuplicate(suffix, f.getFieldName(), v, excludeId);
            if (dup > 0) {
                return DataVo.failure(f.getFieldLabel() + "「" + v + "」已存在（该字段要求唯一）");
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
        List<Map<String, Object>> rows = modelDataDao.findRowsByIds(target, ids, null);
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
            FieldTypeEnum type;
            try {
                type = FieldTypeEnum.of(f.getFieldType());
            } catch (IllegalArgumentException e) {
                continue;
            }
            Object v = row.get(f.getFieldName());
            if (v == null) {
                continue;
            }
            if (type.isStructure()) {
                // P1：结构体内嵌附件子字段的引用同样参与引用计数
                collectStructureAttachmentIds(f, type, String.valueOf(v), ids);
                continue;
            }
            if (!type.isAttachment()) {
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

    /** P1：解析 GROUP/REPEATER 的 JSON 值，收集附件子字段引用的 fly_images.id */
    @SuppressWarnings("unchecked")
    private void collectStructureAttachmentIds(ModelField f, FieldTypeEnum type, String json, List<Long> ids) {
        try {
            List<Map<String, Object>> rows = new ArrayList<>();
            if (type == FieldTypeEnum.GROUP) {
                Map<String, Object> obj = JSON.parseObject(json, Map.class);
                if (obj != null) {
                    rows.add(obj);
                }
            } else {
                List<Object> arr = JSON.parseArray(json);
                for (Object o : arr == null ? List.of() : arr) {
                    if (o instanceof Map) {
                        rows.add((Map<String, Object>) o);
                    }
                }
            }
            if (rows.isEmpty()) {
                return;
            }
            for (ModelField child : modelFieldDao.findFieldsByModelId(f.getModelId(), 1)) {
                if (child.getParentId() == null || child.getParentId() != f.getId()) {
                    continue;
                }
                FieldTypeEnum ct;
                try {
                    ct = FieldTypeEnum.of(child.getFieldType());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!ct.isAttachment()) {
                    continue;
                }
                for (Map<String, Object> r : rows) {
                    Object cv = r.get(child.getFieldName());
                    if (cv == null) {
                        continue;
                    }
                    try {
                        if (ct == FieldTypeEnum.IMAGE || ct == FieldTypeEnum.FILE) {
                            ids.add(Long.parseLong(String.valueOf(cv)));
                        } else {
                            for (String s : parseStringArray(String.valueOf(cv))) {
                                ids.add(Long.parseLong(s.trim()));
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("结构字段[{}]内嵌附件收集失败（忽略）：{}", f.getFieldName(), e.getMessage());
        }
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
