package com.flycms.module.job.service;

import cn.hutool.http.HttpStatus;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.exception.FlycmsException;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.job.dao.JobDao;
import com.flycms.module.job.model.Job;
import com.flycms.module.job.model.JobLog;
import com.flycms.module.job.utils.ScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 定时任务服务类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Slf4j
@Service(value = "jobService")
public class JobService {
    @Autowired
    private JobDao jobDao;

    @Resource(name = "scheduler")
    private Scheduler scheduler;

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////

    /**
     * 新增任务
     *
     * @param job
     */
    public DataVo insertJob(Job job){
        DataVo data = DataVo.failure("操作失败");
        if (!CronExpression.isValidExpression(job.getCronExpression())){
            return data=DataVo.failure("cron表达式格式错误！");
        }
        if(this.checkJobByMethodName(job.getBeanName(),job.getMethodName())){
            return data=DataVo.failure("该任务已存在！");
        }
        SnowFlake snowFlake = new SnowFlake(2, 3);
        job.setId(snowFlake.nextId());
        job.setCreateTime(new Date());
        int totalCount=jobDao.insertJob(job);
        if(totalCount > 0){
            if ("1".equals(job.getStatus())) {
                ScheduleUtils.createScheduleJob(scheduler, job);
            }
            data = DataVo.success("添加成功", DataVo.NOOP);
        }else{
            data=DataVo.failure("未知错误！");
        }
        return data;
    };

    /**
     * 新增任务执行日志
     *
     * @param jobLog
     */
    public void insertJobLog(JobLog jobLog){
        SnowFlake snowFlake = new SnowFlake(2, 3);
        jobLog.setId(snowFlake.nextId());
        jobLog.setCreateTime(new Date());
        jobDao.insertJobLog(jobLog);
    };
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    /**
     * 删除任务
     *
     * @param id
     */
    public DataVo deleteJobById(Long id){
        DataVo data = DataVo.failure("操作失败");
        int totalCount=jobDao.deleteJobById(id);
        if(totalCount > 0){
            CronTrigger trigger = ScheduleUtils.getCronTrigger(scheduler, id);
            if (trigger != null) {
                ScheduleUtils.deleteScheduleJob(scheduler, id);
            }
            data = DataVo.success("已删除!", DataVo.NOOP);
        }else{
            data=DataVo.failure("未知错误！");
        }
        return data;
    };

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 更新任务状态
     *
     * @param job
     */
    public DataVo updateStatus(Job job) {
        DataVo data = DataVo.failure("操作失败");
        int totalCount=jobDao.updateStatus(job.getStatus(),job.getId());
        if(totalCount > 0){
            if ("1".equals(job.getStatus())) {
                CronTrigger trigger = ScheduleUtils.getCronTrigger(scheduler, job.getId());
                if (trigger == null) {
                    ScheduleUtils.createScheduleJob(scheduler, job);
                }else{
                    ScheduleUtils.resumeJob(scheduler, job.getId());
                }
                data = DataVo.success("1", DataVo.NOOP);
            } else {
                ScheduleUtils.pauseJob(scheduler,job.getId());
                data = DataVo.success("0", DataVo.NOOP);
            }
        }else{
            data=DataVo.failure("未知错误！");
        }
        return data;
    }

    /**
     * 更新任务
     *
     * @param job
     */
    public DataVo updateJobById(Job job){
        DataVo data = DataVo.failure("操作失败");
        Job oldJob = this.findJobById(job.getId());
        if(oldJob==null){
            return data = DataVo.failure("该任务不存在");
        }
        if (!CronExpression.isValidExpression(job.getCronExpression())){
            return data=DataVo.failure("cron表达式格式错误！");
        }
        if(this.checkJobByMethodNameNotId(job.getBeanName(),job.getMethodName(),job.getId())){
            return data=DataVo.failure("已存在，不能同时存在两个以上任务！");
        }
        job.setCreateTime(new Date());
        int totalCount=jobDao.updateJobById(job);
        if(totalCount > 0){
            CronTrigger trigger = ScheduleUtils.getCronTrigger(scheduler, job.getId());
            if (trigger == null) {
                ScheduleUtils.createScheduleJob(scheduler, job);
            }else{
                ScheduleUtils.updateScheduleJob(scheduler, job);
            }
            data = DataVo.success("已更新成功!", DataVo.NOOP);
        }else{
            data=DataVo.failure("未知错误！");
        }
        return data;
    };

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////

    /**
     * 按id查询定时任务配置
     *
     * @param id
     * @return
     */
    public Job findJobById(Long id){
        return jobDao.findJobById(id);
    };

    /**
     * 查询定时任务同一个Baen下面的方法是否存在
     *
     * @param beanName
     *          Baen名称
     * @param methodName
     *         方法名称
     * @return
     */
    public boolean checkJobByMethodName(String beanName,String methodName) {
        int totalCount = jobDao.checkJobByMethodName(beanName,methodName);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询定时任务在排除当前id下手否有同一个Baen存在
     *
     * @param beanName
     *          Baen名称
     * @param methodName
     *         方法名称
     * @param id
     *         需要排除的当前id
     * @return
     */
    public boolean checkJobByMethodNameNotId(String beanName,String methodName,Long id) {
        int totalCount = jobDao.checkJobByMethodNameNotId(beanName,methodName,id);
        return totalCount > 0 ? true : false;
    }

    /**
     * 所有启动状态定时任务配置列表
     *
     * @return
     */
    public List<Job> getJobAllList(){
        return jobDao.getJobAllList();
    }

    /**
     * 定时任务配置翻页列表查询
     *
     * @param pageNum
     *         当前页码
     * @param rows
     *         每页数量
     * @return
     */
    public PageVo<Job> getJobListPage(int pageNum, int rows) {
        PageVo<Job> pageVo = new PageVo<Job>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(jobDao.getJobList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(jobDao.getJobCount());
        return pageVo;
    }

    /**
     * 定时任务日志翻页列表查询
     *
     * @param pageNum
     *         当前页码
     * @param rows
     *         每页数量
     * @return
     */
    public PageVo<JobLog> getJobLogListPage(int pageNum, int rows) {
        PageVo<JobLog> pageVo = new PageVo<JobLog>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(jobDao.getJobLogList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(jobDao.getJobLogCount());
        return pageVo;
    }
}
