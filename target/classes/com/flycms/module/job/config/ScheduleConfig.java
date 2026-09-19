package com.flycms.module.job.config;

import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * 定时任务配置
 * @author 郑杰
 */
@Configuration
public class ScheduleConfig {

	/**
	 * 解决Job中注入Spring Bean为null的问题
	 */
	@Component("quartzJobFactory")
	public class QuartzJobFactory extends AdaptableJobFactory {

		/**
		 * 这个对象Spring会帮我们自动注入进来,也属于Spring技术范畴.
		 */
		@Autowired
		private AutowireCapableBeanFactory capableBeanFactory;

		@Override
		protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
			//调用父类的方法
			Object jobInstance = super.createJobInstance(bundle);
			//进行注入,这属于Spring的技术,不清楚的可以查看Spring的API.
			capableBeanFactory.autowireBean(jobInstance);
			return jobInstance;
		}
	}

	/**
	 * 注入scheduler到spring，在下面JobServiceImpl会用到
	 * @param quartzJobFactory
	 * @return
	 * @throws Exception
	 */
	@Bean(name = "scheduler")
	public Scheduler scheduler(QuartzJobFactory quartzJobFactory) throws Exception {

		SchedulerFactoryBean factoryBean=new SchedulerFactoryBean();
		factoryBean.setJobFactory(quartzJobFactory);
		factoryBean.afterPropertiesSet();
		Scheduler scheduler=factoryBean.getScheduler();
		scheduler.start();
		return scheduler;
	}
}
