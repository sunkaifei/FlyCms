package com.flycms.module.job.dao;

import com.flycms.module.job.model.Job;
import com.flycms.module.job.model.JobLog;
import com.flycms.module.other.model.FilterKeyword;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Repository
public interface JobDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 添加定时任务配置
     *
     * @param job
     *         关键词
     * @return
     */
    public int insertJob(Job job);

    public int insertJobLog(JobLog jobLog);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除定时任务配置
    public int deleteJobById(@Param("id") Long id);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 更新定时任务配置审核状态
     *
     * @param job
     *         任务实体类
     * @return
     */
    public int updateJobById(Job job);

    /**
     * 更新定时任务配置审核状态
     *
     * @param status
     *         审核状态
     * @param id
     *         任务id
     * @return
     */
    public int updateStatus(@Param("status") String status, @Param("id") Long id);

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询定时任务配置信息
    public Job findJobById(@Param("id") Long id);

    /**
     * 查询定时任务配置是否存在
     *
     * @param keyword
     *         关键词
     * @return
     */
    public int checkJob(@Param("keyword") String keyword);

    /**
     * 查询定时任务同一个Baen下面的方法是否存在
     *
     * @param beanName
     *          Baen名称
     * @param methodName
     *         方法名称
     * @return
     */
    public int checkJobByMethodName(@Param("beanName") String beanName,@Param("methodName") String methodName);

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
    public int checkJobByMethodNameNotId(@Param("beanName") String beanName,@Param("methodName") String methodName, @Param("id") Long id);

    /**
     *  所有定时任务配置列表
     *
     * @return
     */
    public List<Job> getJobAllList();

    //定时任务配置总数
    public int getJobCount();

    //定时任务配置列表
    public List<Job> getJobList(@Param("offset") Integer offset, @Param("rows") Integer rows);

    //定时任务日志总数
    public int getJobLogCount();

    //定时任务日志列表
    public List<JobLog> getJobLogList(@Param("offset") Integer offset, @Param("rows") Integer rows);
}
