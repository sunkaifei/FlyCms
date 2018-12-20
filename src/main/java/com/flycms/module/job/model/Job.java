package com.flycms.module.job.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 定时任务实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Setter
@Getter
public class Job implements Serializable {

    /**
     * 任务调度参数key
     */
    public static final String JOB_PARAM_KEY = "JOB_PARAM_KEY";

    public enum ScheduleStatus {
        /**
         * 正常
         */
        NORMAL("1"),
        /**
         * 暂停
         */
        PAUSE("0");

        private String value;

        private ScheduleStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private Long id;

    /**
     * Bean名称
     */
    @NotEmpty(message="请填写Bean名称！")
    private String beanName;

    /**
     * 方法名称
     */
    @NotEmpty(message="请填写方法名称！")
    private String methodName;

    /**
     * 参数,执行任务时方法里传递的参数
     */
    private String params;

    /**
     * cron表达式
     */
    @NotEmpty(message="请填写cron表达式！")
    private String cronExpression;

    /**
     * 状态 0=正常,1=暂停
     */
    private String status = "0";

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建日期
     */
    private Date createTime;
}