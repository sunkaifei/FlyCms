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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 栏目树标签（规划阶段 C）。类名 ChannelTree → 注册名 fly_channel_tree，与在线标签手册一致。
 * 模板用法：
 * &lt;@fly_channel_tree fatherId="0"&gt;
 *   &lt;#list channelTree as c&gt;&lt;a href="/${c.channelDir}/"&gt;${c.channelName}&lt;/a&gt;&lt;/#list&gt;
 * &lt;/@fly_channel_tree&gt;
 * params: fatherId（父栏目 id，0=根，默认 0）；输出变量 channelTree（含 children 的树，仅显示中的栏目）
 */
@Service
public class ChannelTree extends AbstractTagPlugin {

    @Autowired
    private ChannelService channelService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        long fatherId = 0;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (String str : paramWrap.keySet()) {
            if ("fatherId".equals(str)) {
                try {
                    fatherId = Long.parseLong(paramWrap.get(str).toString());
                } catch (NumberFormatException ignored) {
                    fatherId = 0;
                }
            }
        }
        env.setVariable("channelTree", builder.build().wrap(subTree(channelService.treeVisible(), fatherId)));
        body.render(env.getOut());
    }

    /** 从树中截取某个父节点下的一层子树；fatherId=0 返回全部根 */
    private List<Channel> subTree(List<Channel> tree, long fatherId) {
        if (fatherId <= 0) {
            return tree == null ? new ArrayList<>() : tree;
        }
        List<Channel> found = findChildren(tree, fatherId);
        return found == null ? new ArrayList<>() : found;
    }

    private List<Channel> findChildren(List<Channel> nodes, long fatherId) {
        if (nodes == null) {
            return null;
        }
        for (Channel c : nodes) {
            if (c.getId() != null && c.getId() == fatherId) {
                return c.getChildren();
            }
            List<Channel> deeper = findChildren(c.getChildren(), fatherId);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }
}
