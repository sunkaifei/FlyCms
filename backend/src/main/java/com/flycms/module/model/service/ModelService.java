package com.flycms.module.model.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.dao.ModelDao;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 内容模型元数据服务：模型 CRUD、code 唯一、缓存（Caffeine "model" 区）、
 * 模板目录自动生成（pc_theme/{skin}/{code}/list.html + detail.html）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ModelService {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ModelService.class);

    private static final String CACHE_NAME = "model";

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private ModelTableService modelTableService;
    @Autowired
    private com.flycms.module.model.dao.ModelCategoryDao modelCategoryDao;
    @Autowired
    private com.flycms.module.model.dao.AutomationRuleDao automationRuleDao;
    @Autowired
    private com.flycms.module.model.dao.ContentVersionDao contentVersionDao;
    @Autowired
    private com.flycms.module.favorite.dao.FavoriteDao favoriteDao;
    @Autowired
    private com.flycms.module.channel.dao.ChannelDao channelDao;
    @Autowired
    private com.flycms.module.template.service.TemplateService templateService;
    @Autowired
    private com.flycms.module.config.service.ConfigService config;
    @Autowired
    private CacheManager cacheManager;

    public Model findModelById(Long id) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            Cache.ValueWrapper w = cache.get("id_" + id);
            if (w != null && w.get() != null) {
                return (Model) w.get();
            }
        }
        Model model = modelDao.findModelById(id);
        if (model != null && cache != null) {
            cache.put("id_" + id, model);
            cache.put("code_" + model.getCode(), model);
        }
        return model;
    }

    public Model findModelByCode(String code) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            Cache.ValueWrapper w = cache.get("code_" + code);
            if (w != null && w.get() != null) {
                return (Model) w.get();
            }
        }
        Model model = modelDao.findModelByCode(code);
        if (model != null && cache != null) {
            cache.put("id_" + model.getId(), model);
            cache.put("code_" + code, model);
        }
        return model;
    }

    public PageVo<Model> getModelListPage(int pageNum, int rows) {
        PageVo<Model> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(modelDao.getModelList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(modelDao.getModelCount());
        return pageVo;
    }

    /** 启用中的模型列表（前台标签/动态菜单用） */
    public List<Model> getEnabledModels() {
        return modelDao.getAllModelList(1);
    }

    /**
     * 新增模型：code 白名单 + 唯一 + 表名冲突预检 → 保存（主键自增回填）→ 建固定列表 → 生成主题模板目录。
     *
     * <p>主键策略（D8）：{@code fly_model.id} 为 AUTO_INCREMENT，由数据库回填，
     * 不再 {@code SnowFlake.nextId()}。业务数据表（fly_cmodel_*）仍用雪花。
     *
     * <p>表名冲突预检（D7）：{@code uk_code} 只能防模型之间重名，防不了与存量表撞名，
     * 故建表前用 {@code tableExistsBySuffix} 再查一次。
     */
    public DataVo addModel(Model model) {
        String code;
        try {
            code = SqlSafeUtil.safeModelCode(model.getCode());
            // 表名后缀独立校验（D10）：路由合法 ≠ 表名合法
            SqlSafeUtil.safeTableSuffix(model.getCode());
        } catch (IllegalArgumentException e) {
            return DataVo.failure(e.getMessage());
        }
        if (modelDao.checkModelCode(code)) {
            return DataVo.failure("模型标识已存在");
        }
        // 表名冲突预检：fly_cmodel_{code} 是否已被占用
        if (modelTableService.tableExists(code)) {
            return DataVo.failure("表名 fly_cmodel_" + code + " 已被占用，请更换模型标识");
        }
        model.setCode(code);
        model.setIsSystem(0);
        model.setStatus(1);
        model.setCreateTime(new Date());
        // 主键自增：不 setId，由 useGeneratedKeys 回填
        if (modelDao.addModel(model) <= 0) {
            return DataVo.failure("保存失败");
        }
        // 建表传 code（物理表名 fly_cmodel_{code}）
        modelTableService.createModelTable(model.getCode(), null);
        generateDefaultTemplates(model);
        evictCache(model.getCode(), model.getId());
        return DataVo.success("模型已创建，数据表与模板已生成", model);
    }

    /**
     * 编辑模型：code 一律锁定（前台路由与表名依赖）；is_system 仅允许改展示属性。
     */
    public DataVo updateModel(Model form) {
        Model old = modelDao.findModelById(form.getId());
        if (old == null) {
            return DataVo.failure("模型不存在");
        }
        form.setCode(old.getCode());
        form.setIsSystem(old.getIsSystem());
        if (modelDao.updateModel(form) > 0) {
            evictCache(old.getCode(), old.getId());
            return DataVo.success("更新成功");
        }
        return DataVo.failure("更新失败");
    }

    /**
     * 删除模型：内置拒绝；级联清理模型维度的一切关联数据——
     * DROP 数据表（内容随之消亡）+ 删字段定义 + 删分类 + 删自动化规则 +
     * 删内容版本快照 + 删收藏 + 解绑栏目（model_id 归 0，栏目转未绑定态）。
     * 菜单/按钮/角色授权节点由 ApiModelController.syncModelMenuNodesOnDelete 负责。
     */
    public DataVo deleteModel(Long id) {
        Model model = modelDao.findModelById(id);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        if (model.getIsSystem() == 1) {
            return DataVo.failure("内置模型不能删除");
        }
        // 物理表名 = fly_cmodel_{code}（D7）
        modelTableService.dropTable(model.getCode());
        modelFieldDao.deleteFieldsByModelId(id);
        modelCategoryDao.deleteByModelId(id);
        automationRuleDao.deleteByModelCode(model.getCode());
        contentVersionDao.deleteByTargetModel(model.getCode());
        favoriteDao.deleteByModelCode(model.getCode());
        channelDao.unbindModel(id);
        evictCache(model.getCode(), id);
        modelDao.deleteModelById(id);
        return DataVo.success("模型已删除，数据表与关联数据已清理");
    }

    public void evictCache(String code, Long id) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            if (id != null) {
                cache.evict("id_" + id);
            }
            if (code != null) {
                cache.evict("code_" + code);
            }
        }
    }

    /**
     * 新模型在当前启用主题下生成 {code}/list.html、{code}/detail.html 默认模板（存在则跳过）。
     *
     * <p><b>D13 骨架自动生成</b>：生成前读取该模型的字段定义，据此挑选"展示字段"，
     * 使新模型开箱即用，不必手写模板。骨架采用完整的页面结构（分类导航 + 分页列表 +
     * 分页条 + 详情正文 + 相关阅读），标签使用 {@code fly_page_model} /
     * {@code fly_info_model} / {@code fly_category_model} / {@code fly_rel_model}，
     * 与存量主题模板（如 {@code pc_theme/defalut/articles/list.html}）保持同一契约。
     *
     * <p><b>相较旧实现的修复</b>：旧骨架用 {@literal <#list dataList as item>} 且**无空值守卫**，
     * 当模型不存在或无数据时 {@code dataList} 为 null，FreeMarker 直接抛
     * {@code InvalidReferenceException}，新模型一访问列表页即 500。新版全部改为
     * {@literal <#if dataList?? && dataList?size gt 0>} 守卫。
     */
    private void generateDefaultTemplates(Model model) {
        writeTemplates(model, false);
    }

    /**
     * E10「重新生成骨架」（D13 收尾）：强制覆盖当前主题下该模型的 list.html / detail.html，
     * 并注入「本模型可用标签 + 实际字段名 + 取值写法」注释块。
     */
    public com.flycms.core.entity.DataVo regenerateDefaultTemplates(Long modelId) {
        Model model = findModelById(modelId);
        if (model == null) {
            return com.flycms.core.entity.DataVo.failure("模型不存在");
        }
        try {
            writeTemplates(model, true);
        } catch (Exception e) {
            logger.warn("重新生成骨架失败（{}）：{}", model.getCode(), e.getMessage());
            return com.flycms.core.entity.DataVo.failure("重新生成失败：" + e.getMessage());
        }
        return com.flycms.core.entity.DataVo.success("骨架已重新生成（list.html / detail.html 已覆盖）");
    }

    private void writeTemplates(Model model, boolean force) {
        // 主题名取自 fly_config_web.keycode=pc_theme（当前为 defalut，历史拼写如此）
        String skin = config.getStringByKey("pc_theme");
        File dir = new File("views/templates/pc_theme/" + skin + "/" + model.getCode());
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        List<ModelField> fields = modelFieldDao.findFieldsByModelId(model.getId(), null);
        writeTemplate(new File(dir, "list.html"), defaultListTemplate(model, fields), force);
        writeTemplate(new File(dir, "detail.html"), defaultDetailTemplate(model, fields), force);
    }

    /** 模型标签速查注释块（E10）：字段感知，列出可用标签与每个字段的取值写法 */
    private String tagCheatSheet(Model model, List<ModelField> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("<#-- ============================================================\n");
        sb.append("     ").append(model.getName()).append("（").append(model.getCode())
                .append("）模型速查 · 自动生成，可随字段增删后点「重新生成骨架」更新\n");
        sb.append("     -----------------------------------------------------------\n");
        sb.append("     可用标签：\n");
        sb.append("       <@fly_page_model model=\"").append(model.getCode())
                .append("\" p=\"${p!1}\" rows=\"10\">  分页列表（dataList/pageHtml）\n");
        sb.append("       <@fly_list_model model=\"").append(model.getCode())
                .append("\" rows=\"5\" orderby=\"count_view\">  非分页列表（dataList）\n");
        sb.append("       <@fly_info_model model=\"").append(model.getCode())
                .append("\" shortUrl=\"${shortUrl!}\">  详情（info）\n");
        sb.append("       <@fly_category_model model=\"").append(model.getCode())
                .append("\">  分类导航（categoryList）\n");
        sb.append("       <@fly_hot_model model=\"").append(model.getCode())
                .append("\" rows=\"8\">  热点排行\n");
        sb.append("       <@fly_rel_model model=\"").append(model.getCode())
                .append("\" category=\"${categoryId}\" notid=\"${info.id}\">  相关内容\n");
        sb.append("     -----------------------------------------------------------\n");
        sb.append("     字段取值（列表行 item / 详情 info）：\n");
        sb.append("       固有：${item.title} ${item.shortUrl}（链接 /").append(model.getCode())
                .append("/${item.shortUrl}.html） ${item.countView} ${(item.createTime)!''}\n");
        if (fields != null) {
            for (ModelField f : fields) {
                if (f.getParentId() != null && f.getParentId() > 0) {
                    continue;
                }
                String fn = f.getFieldName();
                String ft = f.getFieldType();
                String usage;
                switch (ft) {
                    case "image" -> usage = "${(item." + fn + "Url)!''}（附件展开）";
                    case "images", "files" -> usage = "<#list (item." + fn + "Urls)![] as u>${u}</#list>";
                    case "image_url", "file_url" -> usage = "${(item." + fn + ")!''}（URL 直存）";
                    case "relate" -> usage = "${(item." + fn + "Obj.title)!''}（目标行展开）";
                    case "relates" -> usage = "<#list (item." + fn + "List)![] as t>${t.title}</#list>";
                    case "user" -> usage = "${(item." + fn + "Obj.nickName)!''} / 链接 /people/${(item."
                            + fn + "Obj.shortUrl)!''}";
                    case "category" -> usage = "${(item." + fn + "Obj.name)!''}（绑定模型分类树）";
                    case "group" -> usage = "${(item." + fn + ".子字段)!''}（JSON 对象）";
                    case "repeater" -> usage = "<#list (item." + fn + ")![] as row>${row.子字段}</#list>";
                    case "checkbox" -> usage = "${(item." + fn + ")!''}（JSON 数组）";
                    default -> usage = "${(item." + fn + ")!''}";
                }
                sb.append("       ").append(f.getFieldLabel()).append("（").append(fn).append("/").append(ft)
                        .append("）：").append(usage).append("\n");
            }
        }
        sb.append("     ============================================================ -->\n");
        return sb.toString();
    }

    private void writeTemplate(File file, String content, boolean force) {
        if (file.exists() && !force) {
            return;
        }
        try {
            java.nio.file.Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 模型骨架模板写入失败：不阻断建模型主流程，但必须留痕（否则用户只看到"模板文件不存在"）
            logger.warn("写入模型默认模板失败（{}）：{}", file.getPath(), e.getMessage());
        }
    }

    /**
     * 列表页骨架：分类导航 + 分页列表 + 分页条。
     * 字段感知：除 title/shortUrl 固定列外，额外把模型自定义字段（前若干个）拼进摘要区。
     */
    private String defaultListTemplate(Model model, List<ModelField> fields) {
        String code = model.getCode();
        String extra = fieldSnippet(fields, 3);
        return tagCheatSheet(model, fields)
                + "<!DOCTYPE html>\n<html lang=\"zh\">\n<head>\n<meta charset=\"UTF-8\">\n"
                + "<title>${model.name} - ${web_name!''}</title>\n</head>\n<body>\n"
                + "<header><h1><a href=\"/\">${web_name!''}</a> · ${model.name}</h1></header>\n"
                + "<#-- 分类导航（无分类时自动为空） -->\n"
                + "<@fly_category_model model=\"" + code + "\">\n"
                + "<nav><a href=\"/" + code + "/\">全部</a>\n"
                + "<#if categoryList??><#list categoryList as c> <a href=\"/" + code + "/c${c.id}\">${c.name}</a></#list></#if>\n"
                + "</nav>\n</@fly_category_model>\n"
                + "<main>\n"
                + "<@fly_page_model model=\"" + code + "\" p=\"${p!1}\" rows=\"10\">\n"
                + "<ul>\n"
                + "<#if dataList?? && dataList?size gt 0><#list dataList as item>\n"
                + "  <li>\n"
                + "    <a href=\"/" + code + "/${item.shortUrl}.html\">${(item.title)!''}</a>\n"
                + (extra.isEmpty() ? "" : extra)
                + "  </li>\n"
                + "</#list><#else><li>暂无内容</li></#if>\n"
                + "</ul>\n"
                + "<#if pageHtml?? && pageHtml != ''>${pageHtml}</#if>\n"
                + "</@fly_page_model>\n"
                + "</main>\n</body>\n</html>\n";
    }

    /**
     * 详情页骨架：正文 + 相关阅读。
     * 字段感知：按字段类型挑选正文承载字段（editor/textarea 优先）。
     */
    private String defaultDetailTemplate(Model model, List<ModelField> fields) {
        String code = model.getCode();
        String contentField = pickContentField(fields);
        return tagCheatSheet(model, fields)
                + "<!DOCTYPE html>\n<html lang=\"zh\">\n<head>\n<meta charset=\"UTF-8\">\n"
                + "<title>${(info.title)!''} - ${web_name!''}</title>\n</head>\n<body>\n"
                + "<@fly_info_model model=\"" + code + "\" shortUrl=\"${shortUrl!}\">\n"
                + "<article>\n"
                + "<h1>${(info.title)!''}</h1>\n"
                + "<p class=\"meta\">\n"
                + "<#if (info.createTime)??>${info.createTime?substring(0, 16)}</#if>\n"
                + "</p>\n"
                + "<div class=\"content\">${(info." + contentField + ")!''}</div>\n"
                + "</article>\n"
                + "<aside>\n<h3>相关阅读</h3>\n"
                + "<@fly_rel_model model=\"" + code + "\" category=\"${(info.categoryId)!0}\" notid=\"${(info.id)!0}\" rows=\"5\">\n"
                + "<ul>\n"
                + "<#if dataList?? && dataList?size gt 0><#list dataList as item>\n"
                + "  <li><a href=\"/" + code + "/${item.shortUrl}.html\">${(item.title)!''}</a></li>\n"
                + "</#list></#if>\n"
                + "</ul>\n</@fly_rel_model>\n"
                + "</aside>\n"
                + "</@fly_info_model>\n"
                + "</body>\n</html>\n";
    }

    /** 把模型自定义字段拼成一段"元信息"摘要（最多 max 个，跳过 title/content 等主字段）。 */
    private String fieldSnippet(List<ModelField> fields, int max) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (ModelField f : fields) {
            String name = f.getFieldName();
            if (name == null || "title".equals(name) || "content".equals(name)) {
                continue;
            }
            sb.append("    <span class=\"f-").append(name).append("\">${(item.").append(name)
              .append(")!''}</span>\n");
            if (++n >= max) {
                break;
            }
        }
        return sb.toString();
    }

    /** 正文承载字段：优先 editor，其次 textarea，再次 content，最后回退 content。 */
    private String pickContentField(List<ModelField> fields) {
        if (fields != null) {
            for (ModelField f : fields) {
                if ("editor".equalsIgnoreCase(String.valueOf(f.getFieldType()))) {
                    return f.getFieldName();
                }
            }
            for (ModelField f : fields) {
                if ("textarea".equalsIgnoreCase(String.valueOf(f.getFieldType()))) {
                    return f.getFieldName();
                }
            }
            for (ModelField f : fields) {
                if ("content".equalsIgnoreCase(f.getFieldName())) {
                    return f.getFieldName();
                }
            }
        }
        return "content";
    }
}
