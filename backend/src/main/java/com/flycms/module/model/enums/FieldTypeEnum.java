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
    REGION("region", "varchar(64)", null);

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

    /** 是否 JSON 数组类型（checkbox/images/files） */
    public boolean isJsonArray() {
        return this == CHECKBOX || this == IMAGES || this == FILES;
    }

    /** 是否附件引用类型（值 = fly_images.id） */
    public boolean isAttachment() {
        return this == IMAGE || this == IMAGES || this == FILE || this == FILES;
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
