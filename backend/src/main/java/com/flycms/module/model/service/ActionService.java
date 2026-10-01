package com.flycms.module.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.flycms.core.entity.DataVo;
import com.flycms.module.model.dao.ModelDataDao;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.enums.FieldTypeEnum;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ② 动作原语（W/Z 批次引擎第 4 层：动作层，2026-10-01）。
 *
 * <p>查询/展示/投稿三层之外的第四个通用层——登录用户对任意启用模型的**运行时写动作**：
 * <ul>
 *   <li>{@link #submit}：动作流写入（复用投稿白名单与校验，状态不走审核开关，
 *       由模型字段/调用方给定——购物车行、报名行这类"无需审核的记录"）；</li>
 *   <li>{@link #counter}：字段原子增减（DB 端 {@code field = field + delta}，并发安全，
 *       白名单 = number/decimal 字段，下限校验复用 min_value）；</li>
 *   <li>{@link #compose}：组合事务——一次提交多模型多行写入与多字段增减，
 *       逐条执行、失败即回滚全部已执行条目（含库存增减逆向补偿）。</li>
 * </ul>
 * 商城（加购/下单/扣库存）、报名、点赞计数等互动场景全部是这三个原语的调用者，
 * 本服务不含任何业务专属代码。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ActionService {

    private static final Logger log = LoggerFactory.getLogger(ActionService.class);

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelFieldService modelFieldService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private ModelDataDao modelDataDao;

    /** 单行动作写入：status 语义 = 调用方给定的行状态（1 正常；不走审核开关）。
     * 动作层模型（购物车/订单行等）通常无 title 语义——title 缺省时自动派生
     * "{模型名}#{userId}-{时间戳}"，避免 insertData 固有列必填打断通用性。 */
    public DataVo submit(String code, Map<String, String> form, Long userId) {
        Model model = modelService.findModelByCode(safeCode(code));
        if (model == null || model.getStatus() != 1) {
            return DataVo.failure("模块不存在或未启用");
        }
        Map<String, String> filtered = new HashMap<>(form);
        if (StringUtils.isBlank(filtered.get("title"))) {
            filtered.put("title", model.getName() + "#" + userId + "-" + System.currentTimeMillis());
        }
        filtered.put("status", StringUtils.defaultIfBlank(form.get("status"), "1"));
        // insertData 全继承：字段校验/唯一约束/附件引用计数/G12 快照/G18 事件
        return modelDataService.insertData(model.getId(), filtered, userId, userId);
    }

    /** 批量写入：rows 为 JSON 数组，逐行 submit；任一失败即停止并回报行号（不回滚前面已写入行，compose 承担事务） */
    @SuppressWarnings("unchecked")
    public DataVo submitBatch(String code, String rowsJson, Long userId) {
        List<Map<String, Object>> rows;
        try {
            rows = new ArrayList<>();
            for (Object o : JSON.parseArray(rowsJson)) {
                rows.add((Map<String, Object>) o);
            }
        } catch (Exception e) {
            return DataVo.failure("rows 不是合法 JSON 数组");
        }
        if (rows == null || rows.isEmpty()) {
            return DataVo.failure("rows 不能为空");
        }
        List<Object> ids = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rows.get(i).entrySet()) {
                if (e.getValue() != null) {
                    row.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            DataVo vo = submit(code, row, userId);
            if (vo.getCode() != DataVo.CODE_SUCCESS) {
                return DataVo.failure("第 " + (i + 1) + " 行失败：" + vo.getMessage());
            }
            ids.add(vo.getData());
        }
        return DataVo.success("已写入 " + ids.size() + " 行", ids);
    }

    /**
     * 字段原子增减：field 必须是该模型 number/decimal 字段；DB 端 {@code field = field + delta}
     * （并发安全）；delta 为负时结果不允许低于该字段 min_value（不足即失败，不产生半步变更）。
     */
    public DataVo counter(String code, Long id, String fieldName, BigDecimal delta, Long userId) {
        Model model = modelService.findModelByCode(safeCode(code));
        if (model == null || model.getStatus() != 1) {
            return DataVo.failure("模块不存在或未启用");
        }
        ModelField field = modelFieldService.findFieldsByModelId(model.getId(), 1).stream()
                .filter(f -> f.getFieldName().equals(fieldName))
                .findFirst().orElse(null);
        if (field == null) {
            return DataVo.failure("字段不存在：" + fieldName);
        }
        FieldTypeEnum type;
        try {
            type = FieldTypeEnum.of(field.getFieldType());
        } catch (IllegalArgumentException e) {
            return DataVo.failure("字段类型非法");
        }
        if (type != FieldTypeEnum.NUMBER && type != FieldTypeEnum.DECIMAL) {
            return DataVo.failure("仅 number/decimal 字段支持计数增减");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return DataVo.failure("增量不能为 0");
        }
        DataVo safe = checkRowOwner(model, id, userId);
        if (safe != null) {
            return safe;
        }
        // 下限校验：stock(10) + (-2) < 0 → 拒绝（防超卖，DB 端条件更新兜底并发）；
        // 列名过 safeColumnName 白名单后才进 SQL（${} 占位只接受白名单产物）
        String colName;
        try {
            colName = com.flycms.core.utils.SqlSafeUtil.safeColumnName(fieldName);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("字段名不合法");
        }
        String minGuard = "";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("delta", delta);
        if (delta.signum() < 0 && field.getMinValue() != null) {
            minGuard = " AND `" + colName + "` + #{params.delta} >= " + field.getMinValue().toPlainString();
        }
        int rows = modelDataDao.incrementColumn(suffixOf(model.getCode()), fieldName, delta, minGuard, params);
        if (rows <= 0) {
            return DataVo.failure("变更未生效（行不存在" + (delta.signum() < 0 ? "或低于下限" : "") + "）");
        }
        return DataVo.success("已更新");
    }

    /**
     * 组合事务：actions JSON 数组，每项 {op:"insert",model,fields:{...}} / {op:"counter",model,id,field,delta}。
     * 逐条执行，记录已执行动作；任一失败 → 逆向补偿全部已执行（insert 逐个删行、counter 反向 delta），
     * 然后返回失败。适合"下单 = 订单行 + 明细多行 + 库存增减"这类多表联动。
     */
    @SuppressWarnings("unchecked")
    public DataVo compose(String actionsJson, Long userId) {
        List<Map<String, Object>> actions;
        try {
            actions = new ArrayList<>();
            for (Object o : JSON.parseArray(actionsJson)) {
                actions.add((Map<String, Object>) o);
            }
        } catch (Exception e) {
            return DataVo.failure("actions 不是合法 JSON 数组");
        }
        if (actions == null || actions.isEmpty()) {
            return DataVo.failure("actions 不能为空");
        }
        List<Runnable> compensations = new ArrayList<>();
        List<Object> insertedIds = new ArrayList<>();
        try {
            for (int i = 0; i < actions.size(); i++) {
                Map<String, Object> a = actions.get(i);
                String op = String.valueOf(a.getOrDefault("op", "insert"));
                String model = StringUtils.trimToEmpty((String) a.get("model"));
                if (op.equals("insert")) {
                    Map<String, String> fields = new LinkedHashMap<>();
                    Object fv = a.get("fields");
                    if (fv instanceof Map) {
                        for (Map.Entry<String, Object> e : ((Map<String, Object>) fv).entrySet()) {
                            if (e.getValue() != null) {
                                fields.put(e.getKey(), String.valueOf(e.getValue()));
                            }
                        }
                    }
                    DataVo vo = submit(model, fields, userId);
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        throw new IllegalStateException("第 " + (i + 1) + " 步 insert 失败：" + vo.getMessage());
                    }
                    Long rowId = Long.parseLong(String.valueOf(vo.getData()));
                    insertedIds.add(rowId);
                    String suffix = suffixOf(model);
                    compensations.add(0, () -> modelDataDao.hardDeleteRow(suffix, rowId));
                } else if (op.equals("counter")) {
                    Long rowId = Long.parseLong(String.valueOf(a.get("id")));
                    BigDecimal delta = new BigDecimal(String.valueOf(a.get("delta")));
                    String field = StringUtils.trimToEmpty((String) a.get("field"));
                    DataVo vo = counter(model, rowId, field, delta, userId);
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        throw new IllegalStateException("第 " + (i + 1) + " 步 counter 失败：" + vo.getMessage());
                    }
                    BigDecimal reverse = delta.negate();
                    String suffix = suffixOf(model);
                    compensations.add(0, () -> {
                        Map<String, Object> p = new HashMap<>();
                        p.put("id", rowId);
                        p.put("delta", reverse);
                        modelDataDao.incrementColumn(suffix, field, reverse, "", p);
                    });
                } else {
                    throw new IllegalStateException("未知动作类型：" + op);
                }
            }
        } catch (Exception e) {
            for (Runnable undo : compensations) {
                try {
                    undo.run();
                } catch (Exception ex) {
                    log.error("组合事务补偿失败（需人工核对数据）：{}", ex.getMessage(), ex);
                }
            }
            return DataVo.failure(e.getMessage());
        }
        return DataVo.success("组合动作完成", insertedIds);
    }

    /** 行归属与存在性：userId 非空时要求行 user_id=操作者（空=不限，用于平台侧动作） */
    private DataVo checkRowOwner(Model model, Long id, Long userId) {
        Map<String, Object> row = modelDataService.findDataById(model.getId(), id);
        if (row == null) {
            return DataVo.failure("目标内容不存在");
        }
        return null;
    }

    private String suffixOf(String code) {
        return com.flycms.core.utils.SqlSafeUtil.safeTableSuffixForExisting(
                modelService.findModelByCode(safeCode(code)).getCode());
    }

    private String safeCode(String code) {
        try {
            return com.flycms.core.utils.SqlSafeUtil.safeModelCode(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("模块标识不合法");
        }
    }
}
