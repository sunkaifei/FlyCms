package com.flycms.core.utils;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 22:08 2018/7/14
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 校验工具类
 *
 * @author colg
 */
@Component
public class CheckUtils {

    private static MessageSource source;

    /**
     * 在Spring里，静态变量/类变量不是对象的属性，而是一个类的属性，不能用@Autowired一个静态变量（对象），使之成为一个SpringBean。<br />
     *
     * 只能通过setter方法注入，并把类注解成为组件
     *
     *
     * @param source
     */
    @Autowired
    public void init(MessageSource source) {
        CheckUtils.source = source;
    }

    /**
     * 抛出校验错误异常
     *
     * @param msgKey
     * @param args
     */
    private static void fail(String msgKey, Object... args) {
        // 消息的参数化和国际化配置
        Locale locale = LocaleContextHolder.getLocale();
        msgKey = source.getMessage(msgKey, args, locale);
        //throw new CheckException(msgKey);
    }
}