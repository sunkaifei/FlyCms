package com.flycms.module.preset.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.channel.service.ChannelService;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.guide.model.Guide;
import com.flycms.module.model.model.Model;
import com.flycms.module.guide.service.GuideService;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.model.service.ModelTransferService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点预设引擎（P 批次）：把"某类站点的全部组装动作"（分组/模型/字段/分类/设置/栏目/导航/
 * 模板文件/示例内容）打包为 JSON 预设，一键幂等应用——零代码快速建站的核心。
 *
 * <p>幂等口径：分组/栏目/导航/设置按业务键对齐（已存在则跳过或更新），模型/字段委托
 * G20 importModel（code/fieldName 对齐），模板文件默认"存在即跳过"（保护管理员改动，
 * overwrite 选项才覆盖），示例内容带「【示例】」标题前缀（配套一键清空）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class PresetService {

    private static final String PRESET_CLASSPATH = "classpath:presets/*.json";
    private static final String TEMPLATE_CLASSPATH = "presets/templates/";
    private static final String SAMPLE_PREFIX = "【示例】";
    /** 模板写入根（与 ModelService.writeTemplates 同口径，相对后端工作目录） */
    private static final String THEME_ROOT = "views/templates/pc_theme/";

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelTransferService modelTransferService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ChannelService channelService;
    @Autowired
    private GuideService guideService;
    @Autowired
    private ConfigService configService;

    // /////////////////// 查询 ///////////////////

    /** 扫描内置预设（classpath:presets/*.json），返回卡片信息与已应用标记 */
    public List<Map<String, Object>> listInstalled() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (JSONObject json : loadAll()) {
            JSONObject card = json.getJSONObject("preset");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", card.getString("code"));
            item.put("name", card.getString("name"));
            item.put("version", card.getIntValue("version"));
            item.put("icon", card.getString("icon"));
            item.put("description", card.getString("description"));
            item.put("applied", isApplied(json));
            item.put("modelCount", json.getJSONArray("models") == null ? 0 : json.getJSONArray("models").size());
            item.put("templateCount", json.getJSONArray("templates") == null ? 0 : json.getJSONArray("templates").size());
            out.add(item);
        }
        return out;
    }

    /** 预设概览（将创建的对象计数，向导确认页用） */
    public DataVo detail(String code) {
        JSONObject json = loadByCode(code);
        if (json == null) {
            return DataVo.failure("预设不存在：" + code);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("preset", json.getJSONObject("preset"));
        out.put("modelCount", size(json, "models"));
        out.put("fieldCount", countFields(json));
        out.put("categoryCount", countCategories(json));
        out.put("channelCount", size(json, "channels"));
        out.put("guideCount", size(json, "guides"));
        out.put("templateCount", size(json, "templates"));
        out.put("settingsCount", size(json, "settings"));
        out.put("sampleCount", size(json, "sampleContent"));
        return DataVo.success("操作成功", out);
    }

    // /////////////////// 应用 ///////////////////

    /**
     * 幂等应用：分组 → 模型/字段/分类（G20） → 设置 → 栏目 → 导航 → 模板文件 → 示例内容。
     *
     * @param overwriteTemplates true=覆盖已存在模板文件（默认跳过，保护管理员改动）
     * @param withSampleContent  true=插入示例内容（标题带「【示例】」前缀，可一键清空）
     */
    public DataVo apply(String code, boolean overwriteTemplates, boolean withSampleContent, Long adminId) {
        JSONObject json = loadByCode(code);
        if (json == null) {
            return DataVo.failure("预设不存在：" + code);
        }
        List<String> report = new ArrayList<>();
        try {
            // 1) 分组
            JSONArray groups = json.getJSONArray("groups");
            if (groups != null) {
                for (int i = 0; i < groups.size(); i++) {
                    JSONObject g = groups.getJSONObject(i);
                    if (findGroupByCode(g.getString("code")) == null) {
                        com.flycms.module.model.model.ModelGroup form = new com.flycms.module.model.model.ModelGroup();
                        form.setName(g.getString("name"));
                        form.setCode(g.getString("code"));
                        form.setIcon(g.getString("icon"));
                        form.setSort(g.getIntValue("sort"));
                        form.setStatus(1);
                        DataVo vo = modelService.saveGroup(form);
                        report.add("分组[" + g.getString("name") + "] " + (vo.getCode() == DataVo.CODE_SUCCESS ? "已创建" : "创建失败：" + vo.getMessage()));
                    } else {
                        report.add("分组[" + g.getString("name") + "] 已存在，跳过");
                    }
                }
            }

            // 2) 模型/字段/分类（委托 G20 幂等导入）
            JSONArray models = json.getJSONArray("models");
            if (models != null) {
                for (int i = 0; i < models.size(); i++) {
                    JSONObject pack = new JSONObject(new LinkedHashMap<String, Object>());
                    pack.put("model", models.getJSONObject(i).getJSONObject("model"));
                    pack.put("fields", models.getJSONObject(i).getJSONArray("fields"));
                    pack.put("categories", models.getJSONObject(i).getJSONArray("categories"));
                    DataVo vo = modelTransferService.importModel(pack.toJSONString());
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        return DataVo.failure("模型导入失败（" + models.getJSONObject(i).getJSONObject("model").getString("code") + "）：" + vo.getMessage());
                    }
                    String mcode = models.getJSONObject(i).getJSONObject("model").getString("code");
                    report.add("模型[" + mcode + "] 已对齐导入");
                }
            }

            // 3) 设置键（只写缺失，不覆盖管理员已改值）
            JSONArray settings = json.getJSONArray("settings");
            if (settings != null) {
                for (int i = 0; i < settings.size(); i++) {
                    JSONObject s = settings.getJSONObject(i);
                    if (StringUtils.isBlank(configService.getStringByKey(s.getString("key")))) {
                        configService.updagteConfigByKey(s.getString("key"), StringUtils.defaultString(s.getString("value")));
                        report.add("设置[" + s.getString("key") + "] 已写入");
                    }
                }
            }

            // 4) 栏目（dir 对齐；模型绑定）
            JSONArray channels = json.getJSONArray("channels");
            if (channels != null) {
                for (int i = 0; i < channels.size(); i++) {
                    JSONObject c = channels.getJSONObject(i);
                    if (channelService.findByDir(c.getString("dir")) != null) {
                        report.add("栏目[" + c.getString("dir") + "] 已存在，跳过");
                        continue;
                    }
                    Channel form = new Channel();
                    form.setChannelName(c.getString("name"));
                    form.setChannelDir(c.getString("dir"));
                    var boundModel = modelService.findModelByCode(c.getString("model"));
                    form.setModelId(boundModel == null ? 0L : boundModel.getId());
                    form.setChannelType(c.getIntValue("channelType"));
                    form.setListTemplate(StringUtils.defaultString(c.getString("listTemplate")));
                    form.setPageSize(c.getIntValue("pageSize") <= 0 ? 20 : c.getIntValue("pageSize"));
                    form.setStatus(1);
                    DataVo vo = channelService.save(form, adminId);
                    if (vo.getCode() != DataVo.CODE_SUCCESS) {
                        return DataVo.failure("栏目创建失败：" + vo.getMessage());
                    }
                    report.add("栏目[" + c.getString("dir") + "] 已创建");
                }
            }

            // 5) 导航（名称对齐；type=1 引用栏目）
            JSONArray guides = json.getJSONArray("guides");
            if (guides != null) {
                for (int i = 0; i < guides.size(); i++) {
                    JSONObject g = guides.getJSONObject(i);
                    Guide form = new Guide();
                    form.setName(g.getString("name"));
                    form.setType(g.getIntValue("type"));
                    form.setSort(g.getIntValue("sort"));
                    form.setStatus(1);
                    if ("channel".equals(StringUtils.defaultString(g.getString("refKind")))) {
                        Channel ch = channelService.findByDir(g.getString("refChannelDir"));
                        form.setType(1);
                        form.setRefId(ch == null ? 0L : ch.getId());
                    }
                    guideService.save(form);
                    report.add("导航[" + g.getString("name") + "] 已创建");
                }
            }

            // 6) 模板文件（默认存在即跳过）
            JSONArray templates = json.getJSONArray("templates");
            if (templates != null) {
                String skin = StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
                for (int i = 0; i < templates.size(); i++) {
                    String file = templates.getJSONObject(i).getString("file");
                    java.io.File dest = new java.io.File(THEME_ROOT + skin + "/" + file);
                    if (dest.exists() && !overwriteTemplates) {
                        report.add("模板[" + file + "] 已存在，跳过");
                        continue;
                    }
                    String content = loadTemplateFile(code, file);
                    if (content == null) {
                        report.add("模板[" + file + "] 预设内未找到，跳过");
                        continue;
                    }
                    if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                        return DataVo.failure("模板目录创建失败：" + dest.getParent());
                    }
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                        fos.write(content.getBytes(StandardCharsets.UTF_8));
                    }
                    report.add("模板[" + file + "] 已写入");
                }
            }

            // 7) 示例内容（可选；标题带【示例】前缀，可一键清空）
            if (withSampleContent) {
                JSONArray samples = json.getJSONArray("sampleContent");
                if (samples != null) {
                    for (int i = 0; i < samples.size(); i++) {
                        JSONObject s = samples.getJSONObject(i);
                        Model m = modelByCode(s.getString("model"));
                        if (m == null) {
                            continue;
                        }
                        Map<String, String> row = new LinkedHashMap<>();
                        JSONObject r = s.getJSONObject("row");
                        for (String k : r.keySet()) {
                            row.put(k, r.getString(k));
                        }
                        row.put("title", SAMPLE_PREFIX + row.getOrDefault("title", "示例"));
                        row.put("status", "1");
                        modelDataService.insertData(m.getId(), row, adminId, adminId);
                    }
                    report.add("示例内容已插入（可一键清空）");
                }
            }
        } catch (Exception e) {
            return DataVo.failure("应用失败：" + e.getMessage());
        }
        return DataVo.success("预设已应用", report);
    }

    /** 清空示例内容：按「【示例】」前缀删除该预设涉及的模型行 */
    public DataVo clearSample(String code) {
        JSONObject json = loadByCode(code);
        if (json == null) {
            return DataVo.failure("预设不存在：" + code);
        }
        int n = 0;
        JSONArray models = json.getJSONArray("models");
        if (models != null) {
            for (int i = 0; i < models.size(); i++) {
                String mcode = models.getJSONObject(i).getJSONObject("model").getString("code");
                var m = modelByCode(mcode);
                if (m == null) {
                    continue;
                }
                var page = modelDataService.selectPage(m.getId(), SAMPLE_PREFIX, null, null,
                        new HashMap<>(), null, null, 1, 50, null, false, null, null, null, null, false);
                for (Map<String, Object> row : page.getList()) {
                    modelDataService.deleteData(m.getId(), List.of(Long.parseLong(String.valueOf(row.get("id")))));
                    n++;
                }
            }
        }
        return DataVo.success("已清空 " + n + " 条示例内容");
    }

    // /////////////////// 内部 ///////////////////

    /** 已应用判定：分组 code 已存在（预设首个分组为身份锚点） */
    private boolean isApplied(JSONObject json) {
        try {
            JSONArray groups = json.getJSONArray("groups");
            if (groups == null || groups.isEmpty()) {
                return false;
            }
            return findGroupByCode(groups.getJSONObject(0).getString("code")) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private com.flycms.module.model.model.ModelGroup findGroupByCode(String code) {
        for (com.flycms.module.model.model.ModelGroup g : modelService.findGroups(null)) {
            if (g.getCode().equals(code)) {
                return g;
            }
        }
        return null;
    }

    private com.flycms.module.model.model.Model modelByCode(String code) {
        return modelService.findModelByCode(safeCode(code));
    }

    private String safeCode(String code) {
        return code;
    }

    private List<JSONObject> loadAll() {
        List<JSONObject> out = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            for (Resource r : resolver.getResources(PRESET_CLASSPATH)) {
                try (var in = r.getInputStream()) {
                    out.add(JSON.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                } catch (Exception ignored) {
                    // 单个预设文件损坏不阻塞其余
                }
            }
        } catch (IOException ignored) {
        }
        out.sort((a, b) -> Integer.compare(
                a.getJSONObject("preset").getIntValue("version"),
                b.getJSONObject("preset").getIntValue("version")));
        return out;
    }

    private JSONObject loadByCode(String code) {
        for (JSONObject j : loadAll()) {
            if (code.equals(j.getJSONObject("preset").getString("code"))) {
                return j;
            }
        }
        return null;
    }

    private String loadTemplateFile(String presetCode, String file) {
        try (var in = getClass().getClassLoader()
                .getResourceAsStream(TEMPLATE_CLASSPATH + presetCode + "/" + file)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private int size(JSONObject json, String key) {
        JSONArray arr = json.getJSONArray(key);
        return arr == null ? 0 : arr.size();
    }

    private int countFields(JSONObject json) {
        int n = 0;
        JSONArray models = json.getJSONArray("models");
        if (models != null) {
            for (int i = 0; i < models.size(); i++) {
                n += size(models.getJSONObject(i), "fields");
            }
        }
        return n;
    }

    private int countCategories(JSONObject json) {
        int n = 0;
        JSONArray models = json.getJSONArray("models");
        if (models != null) {
            for (int i = 0; i < models.size(); i++) {
                n += size(models.getJSONObject(i), "categories");
            }
        }
        return n;
    }
}
