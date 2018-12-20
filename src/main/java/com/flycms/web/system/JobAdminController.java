package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.job.model.Job;
import com.flycms.module.job.model.JobLog;
import com.flycms.module.job.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author 郑杰
 * @date 2018/10/05 19:12:24
 */
@Slf4j
@Controller
@RequestMapping("/system/job")
public class JobAdminController extends BaseController {

    @Autowired
    private JobService jobService;

    @GetMapping(value = "/list_job")
    public String getJobsInfo(@RequestParam(value = "beanName",required = false) String beanName,
                            @RequestParam(value = "methodName",required = false) String methodName,
                            @RequestParam(value = "remark",required = false) String remark,
                            @RequestParam(value = "status",required = false) String status,
                            @RequestParam(value = "page",defaultValue = "1")Integer page,
                            @RequestParam(value = "limit",defaultValue = "10")Integer limit,
                              ModelMap modelMap){
        PageVo<Job> pageVo=jobService.getJobListPage(page,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("/job/list_job");
    }

    /**
     * 去新增页面
     * @return
     */
    @GetMapping(value = "/add_job")
    public String toAddPage(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("/job/add_job");
    }

    /**
     * 新增
     * @param job
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/insert")
    public DataVo insert(@Valid Job job, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = jobService.insertJob(job);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
            log.warn(e.getMessage());
        }
        return data;
    }


    /**
     * 去编辑页面
     * @return
     */
    @GetMapping(value = "/edit_job/{id}")
    public String toUpdatePage(@PathVariable(value = "id", required = false) String id, ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        Job job = jobService.findJobById(Long.parseLong(id));
        modelMap.put("job", job);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("/job/edit_job");
    }

    /**
     * 更新任务
     * @param job
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/update_job")
    public DataVo update(@Valid Job job, BindingResult result) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = jobService.updateJobById(job);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
            log.warn(e.getMessage());
        }
        return data;
    }

    /**
     * 更新状态
     * @param id
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/update_status")
    public DataVo updateStatus(@RequestParam(value = "id", required = false) String id){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("操作失败");
        }
        Job job = jobService.findJobById(Long.parseLong(id));
        if(job==null){
            return data = DataVo.failure("该任务不存在");
        }
        //如果是关闭状态则开启，如果是开启则关闭
        if("0".equals(job.getStatus())){
            job.setStatus("1");
        }else{
            job.setStatus("0");
        }
        data = jobService.updateStatus(job);
        return data;
    }

    /**
     * 删除任务
     * @param id
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/delete")
    public DataVo delete(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("操作失败");
        }
        Job job = jobService.findJobById(Long.parseLong(id));
        if(job==null){
            return data = DataVo.failure("该任务不存在");
        }
        data = jobService.deleteJobById(Long.parseLong(id));
        return data;
    }

    @GetMapping(value = "/list_joblog")
    public String getJobLogsList(@RequestParam(value = "beanName",required = false) String beanName,
                              @RequestParam(value = "methodName",required = false) String methodName,
                              @RequestParam(value = "remark",required = false) String remark,
                              @RequestParam(value = "status",required = false) String status,
                              @RequestParam(value = "page",defaultValue = "1")Integer page,
                              @RequestParam(value = "limit",defaultValue = "10")Integer limit,
                                 ModelMap modelMap){
        PageVo<JobLog> pageVo=jobService.getJobLogListPage(page,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("/job/list_joblog");
    }
}
