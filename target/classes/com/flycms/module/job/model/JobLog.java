package com.flycms.module.job.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 定时任务日志实体类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Setter
@Getter
public class JobLog implements Serializable {

    private Long id;

    /**
     * 任务id
     */
    private Long jobId;

    /**
     * Bean名称
     */
    private String beanName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 参数
     */
    private String params;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 0=成功,1=失败
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 耗时（毫秒）
     */
    private Long times;

    /**
     * 创建日期
     */
    private Date createTime;

    @Override
    public String toString() {
        return "JobLog{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", beanName='" + beanName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", params='" + params + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", status='" + status + '\'' +
                ", error='" + errorMsg + '\'' +
                ", times=" + times +
                ", createTime=" + createTime +
                '}';
    }
}
