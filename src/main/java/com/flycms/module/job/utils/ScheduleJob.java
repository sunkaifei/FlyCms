package com.flycms.module.job.utils;

import com.flycms.module.job.model.Job;
import com.flycms.module.job.model.JobLog;
import com.flycms.module.job.service.JobService;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 定时任务日志记录操作
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Service
public class ScheduleJob extends QuartzJobBean {
    @Autowired
    private JobService jobService;

    @Resource(name = "scheduler")
    private Scheduler scheduler;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ExecutorService service = Executors.newSingleThreadExecutor();

    @Override
    protected void executeInternal(JobExecutionContext context) {
        Job scheduleJob = (Job) context.getMergedJobDataMap().get(Job.JOB_PARAM_KEY);
        JobLog log = new JobLog();
        log.setJobId(scheduleJob.getId());
        log.setBeanName(scheduleJob.getBeanName());
        log.setMethodName(scheduleJob.getMethodName());
        log.setParams(scheduleJob.getParams());
        long startTime = System.currentTimeMillis();
        log.setCronExpression(scheduleJob.getCronExpression());
        try {
            // 执行任务
            logger.info("任务准备执行，任务ID：{}", scheduleJob.getId());
            ScheduleRunnable task = new ScheduleRunnable(scheduleJob.getBeanName(), scheduleJob.getMethodName(),scheduleJob.getParams());
            Future<?> future = service.submit(task);
            future.get();
            long times = System.currentTimeMillis() - startTime;
            log.setTimes(times);
            // 任务状态 0：失败 1：成功
            log.setStatus("1");
            logger.info("任务执行完毕，任务ID：{} 总共耗时：{} 毫秒", scheduleJob.getId(), times);
        } catch (Exception e) {
            logger.error("任务执行失败，任务ID：" + scheduleJob.getId(), e);
            long times = System.currentTimeMillis() - startTime;
            log.setTimes(times);
            // 任务状态 0：失败 1：成功
            log.setStatus("0");
            log.setErrorMsg(StringUtils.substring(e.toString(), 0, 2000));
            //出错就暂停任务
            scheduleJob.setStatus("0");
            ScheduleUtils.pauseJob(scheduler,scheduleJob.getId());
            //更新状态
            jobService.updateStatus(scheduleJob);
        } finally {
            jobService.insertJobLog(log);
        }
    }
}
