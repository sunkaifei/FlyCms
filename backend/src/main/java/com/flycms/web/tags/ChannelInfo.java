package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.channel.service.ChannelService;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 单个栏目信息标签（规划阶段 C）。类名 ChannelInfo → 注册名 fly_channel_info。
 * 模板用法：
 * &lt;@fly_channel_info dir="${dir!''}"&gt;
 *   &lt;title&gt;${channel.seoTitle!channel.channelName}&lt;/title&gt;
 * &lt;/@fly_channel_info&gt;
 * params: dir（目录名）或 id（栏目 id）；输出变量 channel
 */
@Service
public class ChannelInfo extends AbstractTagPlugin {

    @Autowired
    private ChannelService channelService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        String dir = null;
        Long id = null;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (String str : paramWrap.keySet()) {
            if ("dir".equals(str)) {
                dir = paramWrap.get(str).toString();
            } else if ("id".equals(str)) {
                try {
                    id = Long.parseLong(paramWrap.get(str).toString());
                } catch (NumberFormatException ignored) {
                    id = null;
                }
            }
        }
        Channel channel = null;
        if (id != null && id > 0) {
            channel = channelService.get(id);
        } else if (dir != null && !dir.isEmpty()) {
            channel = channelService.findByDir(dir);
        }
        if (channel != null && channel.getStatus() != 1) {
            channel = null;
        }
        env.setVariable("channel", builder.build().wrap(channel));
        body.render(env.getOut());
    }
}
