package com.flycms.module.channel.service;

import com.flycms.core.entity.PageVo;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.template.service.TemplateCenterService;
import com.flycms.module.template.service.TemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 栏目页渲染（规划 §8 阶段 C）
 *
 * <b>为什么单独抽出渲染器</b>：栏目 URL 形态必须与模型现有形态共存（/{dir}/），
 * 而 ModelController 已占用该 pattern（完全相同的 pattern 会导致 Spring Ambiguous mapping 启动失败）。
 * 因此由 ModelController 与 ChannelController 共同委托本服务渲染，
 * 路由入口分散、渲染逻辑集中，避免两处各写一套导致行为漂移。
 *
 * 模板三级回退：栏目自定义模板 → 模型自定义模板 → 通用 cmodel/channel 模板。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ChannelRenderService {

    @Autowired
    private ChannelService channelService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private TemplateService theme;
    @Autowired
    private ConfigService configService;
    @Autowired
    private TemplateCenterService templateCenterService;
    @Autowired
    private com.flycms.module.template.service.TemplateResolver templateResolver;

    /**
     * 渲染栏目页。
     *
     * @return 视图名（含 redirect: 前缀的外链跳转）；不是栏目返回 null，交由模型逻辑继续处理
     */
    public String render(String dir, int p, ModelMap map) {
        Channel channel = channelService.findByDir(dir);
        if (channel == null || channel.getStatus() != 1) {
            return null;
        }
        // 外链栏目：直接 302，不渲染模板
        if (channel.getChannelType() == 2) {
            return "redirect:" + StringUtils.defaultString(channel.getOutUrl(), "/");
        }
        putCommon(map, channel, p);
        map.addAttribute("children", channelService.offspring(channel.getId()));
        if (channel.getChannelType() == 1) {
            return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.channelPage(channel.getChannelDir()), map);
        }
        if (channel.getChannelType() == 3) {
            return renderAggregate(channel, p, map);
        }
        return renderList(channel, p, map);
    }

    // /////////////////// 内部 ///////////////////

    /** 列表栏目：绑定模型则按模型取数并展开附件；未绑定模型也能建（仅有栏目本身） */
    private String renderList(Channel channel, int p, ModelMap map) {
        Model model = channel.getModelId() > 0 ? modelService.findModelById(channel.getModelId()) : null;
        if (model != null) {
            PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
                    channel.getModelId(), null, null, null, null,
                    "id", "desc", p, channel.getPageSize(), null, true);
            if (pageVo.getList() != null) {
                modelDataService.expandAttachments(channel.getModelId(), pageVo.getList());
            }
            map.addAttribute("dataList", pageVo.getList());
            map.addAttribute("model_page", pageVo);
            map.addAttribute("model", model);
            map.addAttribute("modelCode", model.getCode());
        } else {
            map.addAttribute("dataList", new ArrayList<>());
            map.addAttribute("model_page", new PageVo<>(p));
        }
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.list(
                        model != null ? model.getCode() : null, channel.getChannelDir()), map);
    }

    /**
     * 跨栏目取数并合并排序：<@fly_list_channel> 标签与聚合栏目共用。
     * 每行追加 __modelCode（模板拼详情链接用）。
     *
     * @param channelIds 栏目 id；空表示全站所有列表栏目
     * @param limit      取回条数上限（各栏目各自的上限，非最终结果条数）
     */
    public List<Map<String, Object>> aggregateRows(List<Long> channelIds, int limit) {
        List<Channel> sources = new ArrayList<>();
        if (channelIds == null || channelIds.isEmpty()) {
            sources.addAll(channelService.treeVisible());
        } else {
            for (Long id : channelIds) {
                if (id != null && id > 0) {
                    sources.addAll(channelService.offspring(id));
                }
            }
        }
        List<Map<String, Object>> merged = new ArrayList<>();
        java.util.Set<Long> handled = new java.util.HashSet<>();
        for (Channel c : flatten(sources)) {
            if (c.getChannelType() != 0 || c.getModelId() == null || c.getModelId() == 0
                    || !handled.add(c.getModelId())) {
                continue;
            }
            PageVo<Map<String, Object>> pv = modelDataService.selectPage(
                    c.getModelId(), null, null, null, null, "id", "desc", 1, limit, null, true);
            if (pv.getList() == null) {
                continue;
            }
            modelDataService.expandAttachments(c.getModelId(), pv.getList());
            for (Map<String, Object> row : pv.getList()) {
                row.put("__modelCode", codeOf(c.getModelId()));
                merged.add(row);
            }
        }
        merged.sort(Comparator.comparing(row -> toLong(row.get("id")), Comparator.reverseOrder()));
        return merged;
    }

    /** 树摊平成一维（便于去重同模型的多个栏目） */
    private List<Channel> flatten(List<Channel> list) {
        List<Channel> out = new ArrayList<>();
        for (Channel c : list) {
            out.add(c);
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                out.addAll(flatten(c.getChildren()));
            }
        }
        return out;
    }

    /**
     * 聚合栏目：跨栏目/跨模型混排（承接原「专题」场景，§6.5 的解绑诉求）。
     *
     * 每个子栏目各取一批数据后在内存合并排序再分页——子栏目数量与单栏目取数上限均有界
     * （各取 100 条），适合"专题聚合"这类千级以内的场景；
     * 超大数据量请改用 Listchannel 标签 + 独立分页，或后续接入异步化（阶段 I）。
     */
    private String renderAggregate(Channel channel, int p, ModelMap map) {
        List<Map<String, Object>> merged = aggregateRows(java.util.Collections.singletonList(channel.getId()), 100);
        int rows = channel.getPageSize();
        int from = Math.min((p - 1) * rows, merged.size());
        int to = Math.min(from + rows, merged.size());
        PageVo<Map<String, Object>> pageVo = new PageVo<>(p);
        pageVo.setRows(rows);
        pageVo.setCount(merged.size());
        pageVo.setList(merged.isEmpty() ? new ArrayList<>() : new ArrayList<>(merged.subList(from, to)));
        map.addAttribute("dataList", pageVo.getList());
        map.addAttribute("model_page", pageVo);
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.list(null, channel.getChannelDir()), map);
    }

    private long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String codeOf(Long modelId) {
        Model m = modelId == null || modelId == 0 ? null : modelService.findModelById(modelId);
        return m == null ? "" : StringUtils.defaultString(m.getCode());
    }

    /** 公共变量：栏目本身 + SEO TDK 三级回退（栏目 → 站点配置 → 空） */
    private void putCommon(ModelMap map, Channel channel, int p) {
        map.addAttribute("channel", channel);
        map.addAttribute("dir", channel.getChannelDir());
        map.addAttribute("p", p);
        map.addAttribute("pageSize", channel.getPageSize());
        String siteTitle = StringUtils.defaultIfBlank(configService.getStringByKey("fly_title"), "");
        map.addAttribute("title", StringUtils.defaultIfBlank(channel.getSeoTitle(),
                channel.getChannelName() + (StringUtils.isBlank(siteTitle) ? "" : " - " + siteTitle)));
        map.addAttribute("keywords", StringUtils.defaultIfBlank(channel.getSeoKeywords(),
                StringUtils.defaultIfBlank(configService.getStringByKey("fly_keywords"), "")));
        map.addAttribute("description", StringUtils.defaultIfBlank(channel.getSeoDescription(),
                StringUtils.defaultIfBlank(configService.getStringByKey("fly_description"), "")));
    }

    /**
     * 模板解析已收敛到 {@link com.flycms.module.template.service.TemplateResolver}（规划 D15）：
     * 统一候选链 + 子主题回退，本服务只负责"取数 + 渲染上下文"，不再各自写回退逻辑。
     */
}
