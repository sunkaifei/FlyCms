package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.module.channel.service.ChannelRenderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 统一栏目前台路由（规划 §8 阶段 C）
 *
 * <b>路由形态的选择（踩坑记录）</b>：
 * ModelController 已占用 {@code /{modelCode}/}、{@code /{modelCode}/index}、
 * {@code /{modelCode}/p{page}} 三个 pattern。若栏目也注册完全相同的 pattern，
 * Spring 启动即报 Ambiguous mapping 并退服。因此栏目使用两个<b>与之不同</b>的形态：
 * <ul>
 *   <li>{@code /{channelDir}} —— 不带尾斜杠，与 {@code /{modelCode}/} 不同</li>
 *   <li>{@code /{channelDir}/p{page}/} —— 带尾斜杠，与 {@code /{modelCode}/p{page}} 不同</li>
 * </ul>
 * 至于 {@code /{dir}/} 与 {@code /{dir}/p{n}} 这两种最常见的写法，
 * 由 ModelController 在识别到是栏目时<b>委托</b>给 ChannelRenderService 渲染，
 * 从而四类 URL 全部可用且只存在一份渲染逻辑。
 *
 * 目录名经过与保存时一致的合法性校验后才会查库，避免任意字符串打库。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class ChannelController extends BaseController {

    @Autowired
    private ChannelRenderService channelRenderService;

    @GetMapping(value = {"/{channelDir}", "/{channelDir}/p{page:\\d+}/"})
    public String list(@PathVariable String channelDir,
                       @PathVariable(value = "page", required = false) Integer page,
                       ModelMap modelMap) {
        if (!safeDir(channelDir)) {
            return notFound();
        }
        String view = channelRenderService.render(channelDir, page == null ? 1 : page, modelMap);
        return view == null ? notFound() : view;
    }

    /** 与保存端一致的目录名规则：小写开头，字母数字下划线中划线 */
    private boolean safeDir(String dir) {
        return StringUtils.isNotBlank(dir) && dir.matches("^[a-z][a-z0-9_\\-]{0,49}$");
    }

    private String notFound() {
        return "forward:/404";
    }
}
