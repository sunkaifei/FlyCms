package com.flycms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ServletComponentScan
// 阶段 K1/G1：恢复 ErrorMvcAutoConfiguration（原为 exclude），使未捕获异常有统一的 /error 出口；
// API 侧的结构化 JSON 由 core/exception/GlobalExceptionHandler 接管（仅 web.api 包），
// 前台 web.front 的 /403 /404 /500 主题渲染不受影响。DataSourceAutoConfiguration 仍排除（Druid 手工装配）。
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
//此注解表示动态扫描DAO接口所在包
@MapperScan("com.flycms.module.**.dao")
@EnableCaching
@EnableScheduling  // 定时发布 Job（规划阶段 H）
public class Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
