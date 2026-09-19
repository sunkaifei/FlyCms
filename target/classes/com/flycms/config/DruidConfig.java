package com.flycms.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Druid 数据源配置
 * <p>
 * Spring Boot 4 下 druid-spring-boot-starter 尚未适配（自动配置类使用了旧版
 * Boot 内部 API），改为手动构建 DruidDataSource，并绑定 application.yml 中
 * spring.datasource 前缀下的连接与连接池参数（url/username/password/
 * initialSize/maxActive/filters/connectionProperties 等）。
 */
@Configuration
public class DruidConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }
}
