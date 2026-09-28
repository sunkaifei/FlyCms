package com.flycms.module.job.config;

import com.flycms.module.job.model.Job;
import com.flycms.module.job.service.JobService;
import com.flycms.module.job.utils.ScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author 郑杰
 * @date 2018/10/06 11:09:35
 */
@Slf4j
@Service
public class MyJobRunner implements ApplicationRunner {

    @Autowired
    private JobService jobService;

    @Resource(name = "scheduler")
    private Scheduler scheduler;

    /**
     * 项目启动时重新激活启用的定时任务
     * @param applicationArguments
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments applicationArguments){

        log.info("开始注入启用的定时任务");
        List<Job> jobs = jobService.getJobAllList();
        jobs.forEach(scheduleJob -> {
            ScheduleUtils.createScheduleJob(scheduler, scheduleJob);
        });
    }
}
