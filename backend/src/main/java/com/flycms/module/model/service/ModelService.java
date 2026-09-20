package com.flycms.module.model.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.model.dao.ModelDao;
import com.flycms.module.model.dao.ModelFieldDao;
import com.flycms.module.model.model.Model;
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

    private static final String CACHE_NAME = "model";

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ModelFieldDao modelFieldDao;
    @Autowired
    private ModelTableService modelTableService;
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
     * 新增模型：code 白名单 + 唯一 → 保存 → 建固定列表 → 生成主题模板目录
     */
    public DataVo addModel(Model model) {
        try {
            SqlSafeUtil.safeModelCode(model.getCode());
        } catch (IllegalArgumentException e) {
            return DataVo.failure(e.getMessage());
        }
        if (modelDao.checkModelCode(model.getCode())) {
            return DataVo.failure("模型标识已存在");
        }
        model.setId(new SnowFlake(2, 3).nextId());
        model.setIsSystem(0);
        model.setStatus(1);
        model.setCreateTime(new Date());
        if (modelDao.addModel(model) <= 0) {
            return DataVo.failure("保存失败");
        }
        modelTableService.createModelTable(model.getId(), null);
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
     * 删除模型：内置拒绝；级联 DROP 表 + 删字段定义 + 删分类。
     */
    public DataVo deleteModel(Long id) {
        Model model = modelDao.findModelById(id);
        if (model == null) {
            return DataVo.failure("模型不存在");
        }
        if (model.getIsSystem() == 1) {
            return DataVo.failure("内置模型不能删除");
        }
        modelTableService.dropTable(id);
        modelFieldDao.deleteFieldsByModelId(id);
        evictCache(model.getCode(), id);
        modelDao.deleteModelById(id);
        return DataVo.success("模型已删除，数据表已移除");
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
     * 新模型在当前启用主题下生成 {code}/list.html、{code}/detail.html 默认模板（存在则跳过）
     */
    private void generateDefaultTemplates(Model model) {
        // 主题名取自 fly_config_web.keycode=pc_theme（当前为 defalut，历史拼写如此）
        String skin = config.getStringByKey("pc_theme");
        File dir = new File("views/templates/pc_theme/" + skin + "/" + model.getCode());
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        writeIfAbsent(new File(dir, "list.html"), defaultListTemplate(model));
        writeIfAbsent(new File(dir, "detail.html"), defaultDetailTemplate(model));
    }

    private void writeIfAbsent(File file, String content) {
        if (file.exists()) {
            return;
        }
        try {
            java.nio.file.Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private String defaultListTemplate(Model model) {
        return "<#-- " + model.getName() + " 列表页（自动生成，可自行定制） -->\n"
                + "<@fly_list_model model=\"" + model.getCode() + "\" p=\"${p!1}\" rows=\"10\">\n"
                + "<ul>\n"
                + "<#list dataList as item>\n"
                + "  <li><a href=\"/" + model.getCode() + "/${item.shortUrl}.html\">${item.title}</a></li>\n"
                + "</#list>\n"
                + "</ul>\n"
                + "</@fly_list_model>\n";
    }

    private String defaultDetailTemplate(Model model) {
        return "<#-- " + model.getName() + " 详情页（自动生成，可自行定制） -->\n"
                + "<@fly_info_model model=\"" + model.getCode() + "\" shortUrl=\"${shortUrl!}\">\n"
                + "<h1>${info.title}</h1>\n"
                + "<div>${info.content!''}</div>\n"
                + "</@fly_info_model>\n";
    }
}
