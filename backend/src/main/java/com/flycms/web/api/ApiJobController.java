package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.job.model.Job;
import com.flycms.module.job.service.JobService;
import com.flycms.module.job.utils.ScheduleUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务管理 REST 接口（vben 后台，替换已下线的旧后台 JobAdminController）。
 *
 * 底层复用 module/job 的 Quartz 体系：JobService（CRUD+调度联动）、ScheduleUtils（run/pause/resume）。
 * 权限节点见 sql/stage-l-job-admin.sql（900300 菜单 + 900301~900306 功能点）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system/job")
public class ApiJobController extends ApiBaseController {

    @Autowired
    private JobService jobService;

    @Resource(name = "scheduler")
    private Scheduler scheduler;

    /** 任务分页列表 */
    @ResponseBody
    @GetMapping("/list")
    public DataVo list(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                       @RequestParam(value = "rows", defaultValue = "20") int rows) {
        requirePermission("/api/system/job/list");
        PageVo<Job> pageVo = jobService.getJobListPage(pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }

    /** 新增任务（beanName/methodName/cronExpression/params/remark/status） */
    @ResponseBody
    @PostMapping("/save")
    public DataVo save(@RequestParam(value = "beanName", required = false) String beanName,
                       @RequestParam(value = "methodName", required = false) String methodName,
                       @RequestParam(value = "cronExpression", required = false) String cronExpression,
                       @RequestParam(value = "params", required = false) String params,
                       @RequestParam(value = "remark", required = false) String remark,
                       @RequestParam(value = "status", defaultValue = "0") String status) {
        requirePermission("/api/system/job/save");
        if (StringUtils.isBlank(beanName) || StringUtils.isBlank(methodName)) {
            return DataVo.failure("bean 名称与方法名不能为空");
        }
        if (StringUtils.isBlank(cronExpression)) {
            return DataVo.failure("cron 表达式不能为空");
        }
        Job job = new Job();
        job.setBeanName(beanName.trim());
        job.setMethodName(methodName.trim());
        job.setCronExpression(cronExpression.trim());
        job.setParams(params);
        job.setRemark(remark);
        job.setStatus("1".equals(status) ? "1" : "0");
        return jobService.insertJob(job);
    }

    /** 修改任务（含 cron/参数，调度器同步重建） */
    @ResponseBody
    @PostMapping("/update")
    public DataVo update(@RequestParam(value = "id", required = false) Long id,
                         @RequestParam(value = "beanName", required = false) String beanName,
                         @RequestParam(value = "methodName", required = false) String methodName,
                         @RequestParam(value = "cronExpression", required = false) String cronExpression,
                         @RequestParam(value = "params", required = false) String params,
                         @RequestParam(value = "remark", required = false) String remark) {
        requirePermission("/api/system/job/update");
        if (id == null || id <= 0) {
            return DataVo.failure("任务 id 不能为空");
        }
        if (StringUtils.isBlank(beanName) || StringUtils.isBlank(methodName)
                || StringUtils.isBlank(cronExpression)) {
            return DataVo.failure("bean 名称、方法名、cron 表达式不能为空");
        }
        Job job = new Job();
        job.setId(id);
        job.setBeanName(beanName.trim());
        job.setMethodName(methodName.trim());
        job.setCronExpression(cronExpression.trim());
        job.setParams(params);
        job.setRemark(remark);
        // 保持原状态（启停走独立接口）
        Job old = jobService.findJobById(id);
        job.setStatus(old == null ? "0" : old.getStatus());
        return jobService.updateJobById(job);
    }

    /** 启用/暂停任务（status: 1=启用 0=暂停） */
    @ResponseBody
    @PostMapping("/status")
    public DataVo status(@RequestParam(value = "id", required = false) Long id,
                         @RequestParam(value = "status", required = false) String status) {
        requirePermission("/api/system/job/status");
        if (id == null || id <= 0 || !"0".equals(status) && !"1".equals(status)) {
            return DataVo.failure("参数错误");
        }
        Job job = jobService.findJobById(id);
        if (job == null) {
            return DataVo.failure("任务不存在");
        }
        job.setStatus(status);
        return jobService.updateStatus(job);
    }

    /** 立即执行一次（任务需处于启用状态） */
    @ResponseBody
    @PostMapping("/run")
    public DataVo run(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/job/run");
        if (id == null || id <= 0) {
            return DataVo.failure("任务 id 不能为空");
        }
        Job job = jobService.findJobById(id);
        if (job == null) {
            return DataVo.failure("任务不存在");
        }
        CronTrigger trigger = ScheduleUtils.getCronTrigger(scheduler, id);
        if (trigger == null) {
            return DataVo.failure("任务未启用，请先启用再执行");
        }
        try {
            ScheduleUtils.run(scheduler, job);
            return DataVo.success("已触发执行，结果见执行日志", DataVo.NOOP);
        } catch (Exception e) {
            return DataVo.failure("触发失败：" + e.getMessage());
        }
    }

    /** 删除任务 */
    @ResponseBody
    @PostMapping("/delete")
    public DataVo delete(@RequestParam(value = "id", required = false) Long id) {
        requirePermission("/api/system/job/delete");
        if (id == null || id <= 0) {
            return DataVo.failure("任务 id 不能为空");
        }
        return jobService.deleteJobById(id);
    }

    /** 执行日志分页 */
    @ResponseBody
    @GetMapping("/logList")
    public DataVo logList(@RequestParam(value = "p", defaultValue = "1") int pageNum,
                          @RequestParam(value = "rows", defaultValue = "20") int rows) {
        requirePermission("/api/system/job/logList");
        PageVo<com.flycms.module.job.model.JobLog> pageVo = jobService.getJobLogListPage(pageNum, rows);
        return DataVo.success("操作成功", pageVo);
    }
}
