package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.core.entity.PageVo;
import com.flycms.module.channel.service.ChannelRenderService;
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
 * 聚合内容列表标签（规划阶段 C）。类名 ListChannel → 注册名 fly_list_channel。
 *
 * 跨栏目/跨模型混排，专门承接过去只能靠"专题"硬编码实现的场景（§6.5）。
 * 模板用法：
 * &lt;@fly_list_channel rows="10"&gt;
 *   &lt;#list dataList as row&gt;&lt;li&gt;${row.title}&lt;/li&gt;&lt;/#list&gt;
 * &lt;/@fly_list_channel&gt;
 * params: channelIds（栏目 id，多个逗号分隔，空=全站列表栏目）、rows（条数，默认 10）、p（页码）；
 * 输出变量 dataList（每行含 __modelCode，可直接拼 /${__modelCode}/${shortUrl}.html）、model_page
 */
@Service
public class ListChannel extends AbstractTagPlugin {

    @Autowired
    private ChannelRenderService channelRenderService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        String channelIds = null;
        int rows = 10;
        int p = 1;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (String str : paramWrap.keySet()) {
            String v = paramWrap.get(str).toString();
            if ("channelIds".equals(str)) {
                channelIds = v;
            } else if ("rows".equals(str)) {
                try {
                    rows = Integer.parseInt(v);
                } catch (NumberFormatException ignored) {
                }
            } else if ("p".equals(str)) {
                try {
                    p = Integer.parseInt(v);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (rows <= 0) {
            rows = 10;
        }
        if (p <= 0) {
            p = 1;
        }
        List<Long> ids = parseIds(channelIds);
        // 每个源栏目各取一批后合并排序；取回上限与调用方 rows 挂钩，避免无谓大结果集
        List<Map<String, Object>> all = channelRenderService.aggregateRows(ids, Math.max(rows * 5, 50));
        int from = Math.min((p - 1) * rows, all.size());
        int to = Math.min(from + rows, all.size());
        List<Map<String, Object>> page = all.isEmpty() ? new ArrayList<>() : new ArrayList<>(all.subList(from, to));
        PageVo<Map<String, Object>> pageVo = new PageVo<>(p);
        pageVo.setRows(rows);
        pageVo.setCount(all.size());
        pageVo.setList(page);
        env.setVariable("dataList", builder.build().wrap(page));
        env.setVariable("model_page", builder.build().wrap(pageVo));
        body.render(env.getOut());
    }

    private List<Long> parseIds(String channelIds) {
        List<Long> ids = new ArrayList<>();
        if (channelIds == null || channelIds.trim().isEmpty()) {
            return ids;
        }
        for (String part : channelIds.split(",")) {
            try {
                long v = Long.parseLong(part.trim());
                if (v > 0) {
                    ids.add(v);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}
