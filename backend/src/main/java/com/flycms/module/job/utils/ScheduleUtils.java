package com.flycms.module.job.utils;

import cn.hutool.http.HttpStatus;
import com.flycms.core.exception.FlycmsException;
import com.flycms.module.job.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 定时任务工具类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Slf4j
@Service
public class ScheduleUtils {

    protected ScheduleUtils() {
    }

    private static final String JOB_NAME = "TASK_";

    /**
     * 获取触发器key
     */
    private static TriggerKey getTriggerKey(Long id) {
        return TriggerKey.triggerKey(JOB_NAME + id);
    }

    /**
     * 获取jobKey
     */
    private static JobKey getJobKey(Long id) {
        return JobKey.jobKey(JOB_NAME + id);
    }

    /**
     * 获取表达式触发器
     */
    public static CronTrigger getCronTrigger(Scheduler scheduler, Long jobId) {
        try {
            return (CronTrigger) scheduler.getTrigger(getTriggerKey(jobId));
        } catch (SchedulerException e) {
            log.error("获取Cron表达式失败", e);
        }
        return null;
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, Job scheduleJob) {
        try {
            // 构建job信息
            JobDetail jobDetail = JobBuilder.newJob(ScheduleJob.class).withIdentity(getJobKey(scheduleJob.getId()))
                    .build();

            // 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();

            // 按新的cronExpression表达式构建一个新的trigger
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(scheduleJob.getId()))
                    .withSchedule(scheduleBuilder).build();

            // 放入参数，运行时的方法可以获取
            jobDetail.getJobDataMap().put(Job.JOB_PARAM_KEY, scheduleJob);

            //重置启动时间
            ((CronTriggerImpl)trigger).setStartTime(new Date());

            //执行定时任务
            scheduler.scheduleJob(jobDetail, trigger);

            // 暂停任务
            if (scheduleJob.getStatus().equals(Job.ScheduleStatus.PAUSE.getValue())) {
                pauseJob(scheduler, scheduleJob.getId());
            }
        } catch (SchedulerException e) {
            log.error("创建定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"创建定时任务失败");
        }
    }

    /**
     * 更新定时任务
     */
    public static void updateScheduleJob(Scheduler scheduler, Job scheduleJob) {
        try {
            TriggerKey triggerKey = getTriggerKey(scheduleJob.getId());

            // 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();

            CronTrigger trigger = getCronTrigger(scheduler, scheduleJob.getId());

            if (trigger == null) {
                throw new SchedulerException("获取Cron表达式失败");
            } else {
                // 按新的cronExpression表达式重新构建trigger
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
                // 参数
                trigger.getJobDataMap().put(Job.JOB_PARAM_KEY, scheduleJob);
            }

            //重置启动时间
            ((CronTriggerImpl)trigger).setStartTime(new Date());

            //重置表达式
            scheduler.rescheduleJob(triggerKey, trigger);

            // 暂停任务
            if (scheduleJob.getStatus().equals(Job.ScheduleStatus.PAUSE.getValue())) {
                pauseJob(scheduler, scheduleJob.getId());
            }

        } catch (SchedulerException e) {
            log.error("更新定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"更新定时任务失败");
        }
    }

    /**
     * 立即执行任务
     */
    public static void run(Scheduler scheduler, Job scheduleJob) {
        try {
            // 参数
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(Job.JOB_PARAM_KEY, scheduleJob);

            scheduler.triggerJob(getJobKey(scheduleJob.getId()), dataMap);
        } catch (SchedulerException e) {
            log.error("执行定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"执行定时任务失败");
        }
    }

    /**
     * 暂停任务
     */
    public static void pauseJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.pauseJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            log.error("暂停定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"暂停定时任务失败");
        }
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.resumeJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            log.error("恢复定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"恢复定时任务失败");
        }
    }

    /**
     * 删除定时任务
     */
    public static void deleteScheduleJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.deleteJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            log.error("删除定时任务失败", e);
            throw new FlycmsException(HttpStatus.HTTP_INTERNAL_ERROR,"删除定时任务失败");
        }
    }
}
