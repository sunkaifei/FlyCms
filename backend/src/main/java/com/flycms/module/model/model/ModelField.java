package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 模型字段定义（fly_model_field）。columnType 由后端 FieldTypeEnum 推导冗余存储，前端不可传。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class ModelField implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long modelId;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String columnType;
    private String defaultValue;
    private Integer maxlength;
    private String dictType;
    private String options;
    private int isRequired;
    private int isList;
    private int isSearch;
    private int isFilter;
    /**
     * 表单显示（U3 表单布局补齐）：0=该字段不出现在内容表单（也不校验/写列），
     * 仅存储、列表、详情与模板可见。语义同「条件显隐未命中」的静态版。
     * 缺省 1（显示）——老客户端不传该参数时字段保持可见。
     */
    private int isForm = 1;
    private String regex;
    private String placeholder;
    private String tips;
    /**
     * RELATE / RELATES 字段的目标模型 code（E1）。
     * 留空表示关联「本模型」——即自关联树场景（如回答的 parent_id）。
     */
    private String relateModel;
    /** 值是否全模型唯一（slug/编号等），1=是 */
    private Integer isUnique;
    /** 数值区间下限（number/decimal/rating） */
    private java.math.BigDecimal minValue;
    /** 数值区间上限（number/decimal/rating） */
    private java.math.BigDecimal maxValue;
    /** 表单选项卡名（表单按此分组渲染，组内按 sort 排序） */
    private String tabName;
    /**
     * 父字段 id（P1 结构层）：>0 表示本字段是 GROUP/REPEATER 的子字段，不建物理列。
     * 顶层字段为 0/null。
     */
    private Long parentId;
    /**
     * 条件显隐（P1，对标 ACF Conditional Logic）：JSON
     * {@code {"field":"probe_switch","op":"eq","value":"1"}}。
     * op 白名单：eq/neq/in/notin/empty/notempty。命中隐藏时后端跳过校验与写列。
     */
    private String visibleWhen;
    /**
     * 行内公式（E6，仅 FORMULA 类型）：如 {@code price * 0.88}、{@code (price - cost) * count}。
     * 变量 = 本行字段名；只允许数字/字段名/四则/括号（写入时白名单校验），读取时受限求值不落库。
     */
    private String formula;
    /**
     * Rollup 聚合表达式（P2）：JSON
     * {@code {"source":"brand_id","func":"SUM","column":"price"}}。
     * source 必须是本模型的 relate/relates 字段；func ∈ COUNT/SUM/AVG/MIN/MAX。
     */
    private String rollupExpr;
    /**
     * 行内公式（E6，仅 FORMULA 类型）：如 {@code price * 0.88}。
     * 变量 = 本行字段名；只允许数字/字段名/四则/括号（保存时白名单校验），读取时受限求值不落库。
     */
    /**
     * Lookup 展示列（P2）：JSON 字符串数组，relate/relates 展开目标行（{field}Obj/{field}List）时
     * 额外取回的目标表列，如 ["price","cover"]。列名入库前过 safeColumnName。
     */
    private String lookupFields;
    private int sort;
    private int status;
    private Date createTime;
    private Date updateTime;
}
