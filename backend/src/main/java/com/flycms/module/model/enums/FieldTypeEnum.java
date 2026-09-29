package com.flycms.module.model.enums;

/**
 * 自定义模型字段类型 → MySQL 列定义的唯一合法映射。
 * 列类型只能由这里推导产生（resolveColumnType），禁止接收前端传入的类型串。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public enum FieldTypeEnum {

    INPUT("input", "varchar(%d)", 255),
    TEXTAREA("textarea", "text", null),
    /** 富文本不建独立列，统一走主表 content 通道 */
    EDITOR("editor", null, null),
    NUMBER("number", "bigint(20)", null),
    DECIMAL("decimal", "decimal(18,2)", null),
    DATE("date", "date", null),
    DATETIME("datetime", "datetime", null),
    SELECT("select", "varchar(64)", null),
    RADIO("radio", "varchar(64)", null),
    CHECKBOX("checkbox", "json", null),
    /** 存 fly_images.id */
    IMAGE("image", "bigint(20) unsigned", null),
    /** fly_images.id 数组（JSON） */
    IMAGES("images", "json", null),
    FILE("file", "bigint(20) unsigned", null),
    FILES("files", "json", null),
    /** 地区，数据源 fly_areas（前端暂用文本输入，级联后补） */
    REGION("region", "varchar(64)", null),
    /**
     * 单值关联（E1）：存目标模型内容的 id。目标模型 code 见 fly_model_field.relate_model；
     * relate_model 留空表示关联本模型（自关联树，如回答的 parent_id）。
     */
    RELATE("relate", "bigint(20) unsigned", null),
    /** 多值关联（E1）：id 数组（JSON）。 */
    RELATES("relates", "json", null),
    /**
     * 直存 URL 的图片（E3）：不走 fly_images 引用计数。
     * 用于存量模块（fly_topic.topic_image / fly_links.link_logo）这类「URL 直存」语义，
     * 是《内容体系收敛》§10.3 附件语义冲突的正解——不反查 id，另存 URL。
     */
    IMAGE_URL("image_url", "varchar(500)", null),
    /** 直存 URL 的附件（E3）：不走引用计数。 */
    FILE_URL("file_url", "varchar(500)", null),
    /** 开关（对标 ACF True/False、Strapi Boolean）：0/1 */
    SWITCH("switch", "tinyint(1)", null),
    /** 邮箱（写入时校验格式） */
    EMAIL("email", "varchar(128)", null),
    /** 网址（写入时校验 http/https 前缀） */
    URL("url", "varchar(500)", null),
    /** 中国大陆手机号（写入时校验） */
    PHONE("phone", "varchar(20)", null),
    /** 颜色值（#RGB/#RRGGBB/#RRGGBBAA） */
    COLOR("color", "varchar(16)", null),
    /** 评分（1..5，可配 min/max 扩展量程） */
    RATING("rating", "tinyint(4)", null),
    /** URL 片段（对标 Strapi UID/ACF 的 slug：小写字母数字连字符，常配唯一约束） */
    SLUG("slug", "varchar(128)", null),
    /**
     * 子字段组（P1 结构层，对标 ACF Group / Strapi Component）：JSON 列存对象，
     * 子字段定义存 fly_model_field.parent_id（字段表自关联），录入与展示按子字段 schema 递归处理。
     */
    GROUP("group", "json", null),
    /**
     * 重复行（P1 结构层，对标 ACF Repeater / Payload Array）：与 GROUP 同构，
     * JSON 列存数组，每行按子字段 schema 校验。
     */
    REPEATER("repeater", "json", null),
    /**
     * 聚合统计（P2 关系层，对标 NocoDB Rollup）：虚拟字段不建列，
     * 配置存 fly_model_field.rollup_expr（JSON：source=本模型关联字段名，func=COUNT/SUM/AVG/MIN/MAX，
     * column=SUM/AVG/MIN/MAX 时目标行的数值列名，须过 safeColumnName）。
     */
    ROLLUP("rollup", null, null),
    /**
     * 多对任意（P2 关系层，对标 Directus M2A）：不建物理列，关系存平台中间表 fly_relation
     * （from_model/from_id/field_name/to_model/to_id/sort），值 = [{model,id},…] 数组，
     * 承接「相关阅读/商品推荐」这类可关联任意模型的场景。
     */
    M2A("m2a", null, null),
    /**
     * 计算列（E6，对标 Excel 公式/ACF 计算字段）：虚拟字段不落列，
     * 配置存 fly_model_field.formula（行内表达式，如 {@code price * 0.88}），
     * 读取时以本行字段值为变量求值（SpEL SimpleEvaluationContext 受限求值，仅四则+括号+数字+字段名）。
     */
    FORMULA("formula", null, null),
    /**
     * 用户选择器（B1 绑定数据源，对标 ACF User 字段）：存 fly_user.user_id。
     * 表单渲染可搜索的用户下拉（显示昵称/用户名），提交保存 user_id；
     * 读侧展开 {@code {field}Obj = {userId, nickName, avatar, shortUrl}}，模板可直接输出用户链接。
     */
    USER("user", "bigint(20) unsigned", null),
    /**
     * 分类选择器（B1 绑定数据源，对标 ACF Taxonomy 字段）：存 fly_model_category.id。
     * 表单渲染绑定模型的分类树下拉；绑定目标由 fly_model_field.relate_model 决定，
     * 留空 = 本模型分类树（与主表固定 category_id 通道互补，可做副分类/多维度归类）。
     * 读侧展开 {@code {field}Obj = {id, name, fatherId}}。
     */
    CATEGORY("category", "bigint(20) unsigned", null);

    private final String code;
    private final String ddl;
    private final Integer defaultLength;

    FieldTypeEnum(String code, String ddl, Integer defaultLength) {
        this.code = code;
        this.ddl = ddl;
        this.defaultLength = defaultLength;
    }

    public String getCode() {
        return code;
    }

    public String getDdl() {
        return ddl;
    }

    public Integer getDefaultLength() {
        return defaultLength;
    }

    /** 是否建独立列（editor 走 content 通道不建列） */
    public boolean hasColumn() {
        return ddl != null;
    }

    /** 是否 JSON 数组类型（checkbox/images/files/relates） */
    public boolean isJsonArray() {
        return this == CHECKBOX || this == IMAGES || this == FILES || this == RELATES;
    }

    /** 是否附件引用类型（值 = fly_images.id，参与引用计数） */
    public boolean isAttachment() {
        return this == IMAGE || this == IMAGES || this == FILE || this == FILES;
    }

    /**
     * 是否关联引用类型（E1：值 = 目标模型的业务数据 id）。
     * 目标模型 code 取 {@code fly_model_field.relate_model}；为空表示关联本模型。
     */
    public boolean isRelation() {
        return this == RELATE || this == RELATES;
    }

    /** 是否「直存 URL」附件类型（E3：值就是 URL 字符串，不参与引用计数） */
    public boolean isUrlType() {
        return this == IMAGE_URL || this == FILE_URL;
    }

    /** 是否结构类型（P1：GROUP/REPEATER，JSON 存子字段结构，子字段定义见 parent_id） */
    public boolean isStructure() {
        return this == GROUP || this == REPEATER;
    }

    /**
     * 是否「绑定平台数据源」类型（B1）：值是平台实体的 id（fly_user / fly_model_category），
     * 与 isRelation（绑其他模型内容）并列的第三类引用语义。
     * 绑定配置：USER 无需配置；CATEGORY 的绑定模型 code 复用 relate_model（留空 = 本模型）。
     */
    public boolean isBoundEntity() {
        return this == USER || this == CATEGORY;
    }

    /** 是否虚拟字段（不建列、不落动态表：rollup/m2a/formula，editor 走主表 content 通道同样无列） */
    public boolean isVirtual() {
        return this == ROLLUP || this == M2A || this == FORMULA;
    }

    /** 是否允许作为 GROUP/REPEATER 的子字段（结构/虚拟/富文本不可嵌套，编辑器走主表通道会撞 content） */
    public boolean canBeStructureChild() {
        return !isStructure() && !isVirtual() && this != EDITOR;
    }

    /** 是否 text 系重列（列表查询不投影）：textarea 是 text，editor 走主表 content 通道 */
    public boolean isHeavyText() {
        return this == TEXTAREA || this == EDITOR;
    }

    /**
     * 由字段类型推导列定义；maxlength 仅对可变长类型生效，超限截断到 4000
     */
    public String resolveColumnType(Integer maxlength) {
        if (ddl == null) {
            return null;
        }
        if (ddl.contains("%d")) {
            int len = maxlength == null ? defaultLength : Math.min(maxlength, 4000);
            return String.format(ddl, len);
        }
        return ddl;
    }

    /** 按 code 查枚举，查不到抛 IllegalArgumentException（前端传值只允许枚举 code） */
    public static FieldTypeEnum of(String code) {
        for (FieldTypeEnum t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知字段类型：" + code);
    }
}
