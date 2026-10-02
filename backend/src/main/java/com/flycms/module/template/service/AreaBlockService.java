package com.flycms.module.template.service;

import com.alibaba.fastjson2.JSON;
import com.flycms.core.entity.DataVo;
import com.flycms.module.template.dao.AreaBlockDao;
import com.flycms.module.template.model.AreaBlock;
import com.flycms.module.template.model.Theme;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 区域编排服务（规划 §7.3 / §8.4 V2 / §12.1 / P10）。
 *
 * <p><b>核心思想</b>：布局从代码变成数据。模板写 {@code <@fly_area name="content_top"/>} 只声明
 * "这里可以放东西"，放什么、什么顺序由后台决定 —— Drupal regions + Shopify sections 的轻量结合。
 *
 * <p><b>三类区块</b>（与 {@code fly_area_block.block_type} 对应）：
 * <ul>
 *   <li>{@code BLOCK} — 引用碎片（{@code block_ref} = {@code fly_block.block_key}），
 *       渲染成 {@code <@fly_block>} 的等价模板片段，<b>复用</b>已有的碎片标签与缓存；</li>
 *   <li>{@code TAG} — 一段 FreeMarker 代码（如 {@code <@fly_list_model model="articles" rows="5">…}）；</li>
 *   <li>{@code HTML} — 自定义 HTML，原样输出。</li>
 * </ul>
 *
 * <p><b>统一渲染</b>：三类区块最终都被拼成"一段模板源码"，一次性交给 FreeMarker 渲染。
 * 好处是标签作用域、沙箱（{@code ALLOWS_NOTHING_RESOLVER}）、缓存策略全部与模板页一致，
 * 不会出现"后台配的东西走了一条没人审计的渲染捷径"。
 *
 * <p><b>永不崩页</b>：区块配置写坏（标签语法错、碎片不存在）时只记录告警并跳过，
 * 绝不把异常抛给前台 —— 后台配置错误不该让访客看到 500。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class AreaBlockService {

    private static final Logger logger = LoggerFactory.getLogger(AreaBlockService.class);

    /** 单个区域的区块数量上限（防止后台误配上百个区块把首页拖死） */
    private static final int MAX_BLOCKS = 50;

    /** 单段区块源码长度上限（与 TemplateCenterService 的模板大小限制同量级） */
    private static final int MAX_REF_LEN = 200_000;

    /** 区域名白名单：字母数字下划线中划线 */
    private static final java.util.regex.Pattern AREA_NAME =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_\\-]{1,50}$");

    @Autowired
    private AreaBlockDao areaBlockDao;
    @Autowired(required = false)
    private ThemeRegistry registry;
    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    // /////////////////// 后台 CRUD ///////////////////

    /** 某主题全部已编排区块（后台布局管理页数据源） */
    public List<AreaBlock> listByTheme(String themeCode) {
        String code = StringUtils.defaultIfBlank(themeCode, currentTheme());
        try {
            List<AreaBlock> list = areaBlockDao.listByTheme(code);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            logger.warn("读取区域区块失败：theme={}，原因={}", code, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 该主题"可用的区域清单" = {@code theme.json} 的 {@code supports.regions}
     * ＋ 已经编排过区块的区域名（并集，去重，稳定排序）。
     *
     * <p>前者是主题声明的"模板里真的有占位"，后者是"后台已经用过"——
     * 只给前者会让已有数据无处显示，只给后者会让新建区域无从下手。
     */
    public List<Map<String, Object>> regions(String themeCode) {
        String code = StringUtils.defaultIfBlank(themeCode, currentTheme());
        Set<String> declared = new HashSet<>();
        if (registry != null) {
            Theme t = registry.getTheme(code);
            if (t != null && t.getRegions() != null) {
                declared.addAll(t.getRegions());
            }
        }
        Set<String> used = new HashSet<>();
        try {
            List<String> names = areaBlockDao.listAreaNames(code);
            if (names != null) {
                used.addAll(names);
            }
        } catch (Exception e) {
            logger.debug("读取已用区域名失败：{}", e.getMessage());
        }
        java.util.TreeSet<String> all = new java.util.TreeSet<>();
        all.addAll(declared);
        all.addAll(used);

        List<Map<String, Object>> out = new ArrayList<>();
        for (String name : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("declared", declared.contains(name));
            m.put("blockCount", countInArea(code, name));
            out.add(m);
        }
        return out;
    }

    private int countInArea(String themeCode, String areaName) {
        try {
            List<AreaBlock> list = areaBlockDao.listEnabled(themeCode, areaName);
            return list == null ? 0 : list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 新增区块（sort 缺省时追加到该区域末尾） */
    public DataVo add(AreaBlock block) {
        DataVo invalid = validate(block);
        if (invalid != null) {
            return invalid;
        }
        try {
            if (block.getSort() == null) {
                Integer max = areaBlockDao.maxSort(block.getThemeCode(), block.getAreaName());
                block.setSort(max == null ? 0 : max + 10);
            }
            if (block.getStatus() == null) {
                block.setStatus(1);
            }
            areaBlockDao.insert(block);
            return DataVo.success("区块已添加");
        } catch (Exception e) {
            logger.warn("添加区域区块失败：{}", e.getMessage());
            return DataVo.failure("添加失败：" + e.getMessage());
        }
    }

    /** 更新区块（标题/内容/启用状态） */
    public DataVo update(AreaBlock block) {
        if (block == null || block.getId() == null) {
            return DataVo.failure("缺少区块 id");
        }
        if (StringUtils.isNotBlank(block.getBlockRef()) && block.getBlockRef().length() > MAX_REF_LEN) {
            return DataVo.failure("区块内容过长（上限 " + MAX_REF_LEN + " 字符）");
        }
        if (StringUtils.isNotBlank(block.getAreaName()) && !AREA_NAME.matcher(block.getAreaName()).matches()) {
            return DataVo.failure("区域名只允许字母/数字/下划线/中划线");
        }
        try {
            areaBlockDao.update(block);
            return DataVo.success("区块已保存");
        } catch (Exception e) {
            logger.warn("保存区域区块失败：{}", e.getMessage());
            return DataVo.failure("保存失败：" + e.getMessage());
        }
    }

    public DataVo delete(Long id) {
        if (id == null) {
            return DataVo.failure("缺少区块 id");
        }
        try {
            areaBlockDao.deleteById(id);
            return DataVo.success("区块已删除");
        } catch (Exception e) {
            return DataVo.failure("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量重排（拖拽后一次性提交顺序）。
     *
     * @param ids 按新顺序排列的区块 id
     */
    public DataVo reorder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return DataVo.failure("没有需要排序的区块");
        }
        try {
            int sort = 0;
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                areaBlockDao.updateSort(id, sort);
                sort += 10;
            }
            return DataVo.success("顺序已保存");
        } catch (Exception e) {
            return DataVo.failure("排序失败：" + e.getMessage());
        }
    }

    public DataVo toggleStatus(Long id, Integer status) {
        if (id == null) {
            return DataVo.failure("缺少区块 id");
        }
        try {
            areaBlockDao.updateStatus(id, status == null || status != 0 ? 1 : 0);
            return DataVo.success("状态已更新");
        } catch (Exception e) {
            return DataVo.failure("更新失败：" + e.getMessage());
        }
    }

    /** 校验新增入参 */
    private DataVo validate(AreaBlock block) {
        if (block == null) {
            return DataVo.failure("参数为空");
        }
        if (StringUtils.isBlank(block.getAreaName())
                || !AREA_NAME.matcher(block.getAreaName()).matches()) {
            return DataVo.failure("区域名必填，且只允许字母/数字/下划线/中划线");
        }
        String type = block.getBlockType();
        if (!AreaBlock.TYPE_BLOCK.equals(type) && !AreaBlock.TYPE_TAG.equals(type)
                && !AreaBlock.TYPE_HTML.equals(type)) {
            return DataVo.failure("区块类型只支持 BLOCK / TAG / HTML");
        }
        if (StringUtils.isBlank(block.getBlockRef())) {
            return DataVo.failure("区块内容不能为空");
        }
        if (block.getBlockRef().length() > MAX_REF_LEN) {
            return DataVo.failure("区块内容过长（上限 " + MAX_REF_LEN + " 字符）");
        }
        if (StringUtils.isBlank(block.getThemeCode())) {
            block.setThemeCode(currentTheme());
        }
        return null;
    }

    // /////////////////// 前台渲染 ///////////////////

    /** 区域渲染结果：结构化区块 + 拼好的 HTML，模板按需取用 */
    public static class AreaView {
        private String name;
        private List<AreaBlock> blocks = new ArrayList<>();
        private String html = "";

        public String getName() {
            return name;
        }

        public List<AreaBlock> getBlocks() {
            return blocks;
        }

        public String getHtml() {
            return html;
        }
    }

    /**
     * 渲染指定区域（前台 {@code <@fly_area name="…"/>} 的入口）。
     *
     * @param themeCode 主题 code，空则取当前主题
     * @param areaName  区域名
     */
    public AreaView renderArea(String themeCode, String areaName) {
        AreaView view = new AreaView();
        view.name = areaName;
        if (StringUtils.isBlank(areaName) || !AREA_NAME.matcher(areaName).matches()) {
            return view;
        }
        String code = StringUtils.defaultIfBlank(themeCode, currentTheme());
        List<AreaBlock> blocks;
        try {
            blocks = areaBlockDao.listEnabled(code, areaName);
        } catch (Exception e) {
            logger.warn("读取区域 [{}] 的区块失败：{}", areaName, e.getMessage());
            return view;
        }
        if (blocks == null || blocks.isEmpty()) {
            return view;
        }
        if (blocks.size() > MAX_BLOCKS) {
            logger.warn("区域 [{}] 配置了 {} 个区块，超出上限 {}，只渲染前 {} 个",
                    areaName, blocks.size(), MAX_BLOCKS, MAX_BLOCKS);
            blocks = blocks.subList(0, MAX_BLOCKS);
        }
        view.blocks = blocks;

        StringBuilder src = new StringBuilder();
        Map<String, List<Map<String, Object>>> meta = new LinkedHashMap<>();
        for (AreaBlock b : blocks) {
            String snippet = toSnippet(b);
            if (StringUtils.isBlank(snippet)) {
                continue;
            }
            src.append("\n<!-- area-block:").append(b.getBlockType())
                    .append(" #").append(b.getId()).append(" -->\n");
            // S1-a 区块属性：wrapper_class 非空时包一层 div（多类空格分隔，字符白名单防注入）
            if (StringUtils.isNotBlank(b.getWrapperClass())) {
                String cls = b.getWrapperClass().replaceAll("[^A-Za-z0-9_ \\\\-]", "");
                src.append("<div class=\"").append(cls).append("\">");
                src.append(snippet);
                src.append("</div>");
            } else {
                src.append(snippet);
            }
        }
        if (src.length() == 0) {
            return view;
        }
        view.html = render(src.toString(), meta);
        return view;
    }

    /**
     * 把区块转成"模板源码片段"。
     *
     * <p>{@code BLOCK} 类型刻意复用 {@code <@fly_block>} 而不是直接读碎片数据 ——
     * 这样碎片的渲染缓存、时间窗过滤、条目限量等既有语义全部自动继承，
     * 不会出现"后台配的碎片与模板里写的碎片表现不一致"。
     */
    private String toSnippet(AreaBlock b) {
        String ref = b.getBlockRef();
        if (StringUtils.isBlank(ref)) {
            return null;
        }
        if (AreaBlock.TYPE_HTML.equals(b.getBlockType())) {
            return ref;
        }
        if (AreaBlock.TYPE_TAG.equals(b.getBlockType())) {
            return ref;
        }
        // BLOCK：展开成 fly_block 的标准用法
        String key = ref.trim();
        if (!key.matches("^[A-Za-z0-9_\\-]{1,50}$")) {
            logger.warn("区域区块 #{} 引用的碎片 key 非法：{}", b.getId(), key);
            return null;
        }
        return "<@fly_block key=\"" + key + "\">"
                + "<#if block?? && block.blockType == 2>"
                + "<ul class=\"area-list\"><#list block.items as it>"
                + "<li><a href=\"${it.url!'/'}\" title=\"${(it.title!'')?html}\">${(it.title!'')?html}</a></li>"
                + "</#list></ul>"
                + "<#elseif block??>${block.content!''}</#if>"
                + "</@fly_block>";
    }

    /** 把拼接好的区块源码当模板渲染；任何异常都降级为空串 + 告警 */
    private String render(String source, Map<String, List<Map<String, Object>>> model) {
        try {
            Configuration cfg = freeMarkerConfigurer.getConfiguration();
            Template tpl = new Template("__area__", new StringReader(source), cfg);
            StringWriter out = new StringWriter();
            tpl.process(model == null ? new HashMap<>() : model, out);
            return out.toString();
        } catch (Exception e) {
            logger.warn("区域区块渲染失败，已降级为空：{}", firstLine(e.getMessage()));
            return "";
        }
    }

    /** 后台预览：把某区域渲染结果返回给 UI（保存后立刻看效果） */
    public DataVo preview(String themeCode, String areaName) {
        AreaView v = renderArea(themeCode, areaName);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("area", areaName);
        data.put("html", v.html);
        data.put("blockCount", v.blocks.size());
        return DataVo.success("操作成功", data);
    }

    /** 区块清单的 JSON（供 UI 复制/迁移用） */
    public String exportJson(String themeCode) {
        return JSON.toJSONString(listByTheme(themeCode));
    }

    // /////////////////// 内部 ///////////////////

    private String currentTheme() {
        if (registry != null) {
            return registry.currentSkin();
        }
        return "defalut";
    }

    private String firstLine(String msg) {
        if (msg == null) {
            return "未知错误";
        }
        int i = msg.indexOf('\n');
        return i < 0 ? msg : msg.substring(0, i);
    }
}
