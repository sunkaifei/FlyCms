package com.flycms.module.job.service.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 任务指定bean
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Slf4j
@Component
public class MyTaskTest {

    public void test(String params) {
        log.info("我是带参数的test方法，正在被执行，参数为：{}" , params);
    }

    public void test1() {
        // U3：文章权重任务随 module/weight 退役，仅保留演示任务空实现
        log.info("我是不带参数的test1方法，正在被执行");
    }
}
