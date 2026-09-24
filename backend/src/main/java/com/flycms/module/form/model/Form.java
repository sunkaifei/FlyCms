package com.flycms.module.form.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 表单（规划阶段 F）
 *
 * 表单定义与内容模型的分工：模型数据要进列表查询/排序/SEO 详情页，用真实列+索引；
 * 表单数据只需后台查看与导出，用统一表 + JSON，避免动态建表数量失控。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class Form implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 雪花 ID */
    private Long id;
    /** 调用码，前台提交 /api/form/submit/{formCode} */
    private String formCode;
    /** 表单名称 */
    private String formName;
    /** 提交是否需审核 0否 1是 */
    private Integer audit;
    /** 同一用户/IP 每日限提次数 */
    private Integer submitLimit;
    /** 是否需要验证码 0否 1是 */
    private Integer needCaptcha;
    /** 提交成功提示语 */
    private String successTip;
    /** 提交通知邮箱，空=不通知 */
    private String notifyEmail;
    /** 状态 0停用 1启用 */
    private Integer status;
    private Date createTime;

    /** 非表字段：字段列表（标签 <@fly_form> 输出用） */
    private java.util.List<FormField> fields;
    /** 非表字段：字段数量（列表展示用） */
    private Integer fieldCount;
    /** 非表字段：已收集数据条数（列表展示用） */
    private Integer dataCount;
}
