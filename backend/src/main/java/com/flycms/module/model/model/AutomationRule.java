package com.flycms.module.model.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 自动化规则（G17，Directus Flows 简化版）：规则表 + 内置动作集，非可视化编排。
 *
 * <p>监听 {@link com.flycms.core.event.ContentChangedEvent}（G18 契约）：
 * event + model_code 匹配 → conditions 对内容首行求值（可选）→ 依次执行 actions
 * （内置 webhook / log；动作异常互不影响、不阻断主流程）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Setter
@Getter
public class AutomationRule implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String ruleName;
    /** 匹配事件：insert/update/delete/status/comment_add/comment_audit/comment_delete */
    private String event;
    /** 限定模型 code，空 = 全部模型 */
    private String modelCode;
    /** JSON [{field,op,value}]，op ∈ eq/neq/gt/lt/contains；对首行内容求值；空 = 无条件 */
    private String conditions;
    /** JSON [{type:"webhook",url:...} | {type:"log"}] */
    private String actions;
    private Integer status;
    private Date createTime;
}
