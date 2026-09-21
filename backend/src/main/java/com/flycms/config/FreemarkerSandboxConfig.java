package com.flycms.config;

import freemarker.core.TemplateClassResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import jakarta.annotation.PostConstruct;

/**
 * Freemarker 模板沙箱（规划 §6.1 / 阶段 A1，P0）：
 * 模板在线编辑等价于"可写代码"，历史上是 Dede/PHPCMS 的 RCE 重灾区。
 * 禁止模板内 ?new 实例化任何类（ALLOWS_NOTHING_RESOLVER），
 * 模板数据获取只允许已注册的 <@fly_xxx> 指令标签与 Freemarker 内建。
 * 存量模板已核查无 ?new 用法（启用前 grep views/templates）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Component
public class FreemarkerSandboxConfig {

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @PostConstruct
    public void applySandbox() {
        freeMarkerConfigurer.getConfiguration()
                .setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
    }
}
