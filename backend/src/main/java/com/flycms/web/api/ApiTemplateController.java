package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.module.template.dao.TemplateAssignDao;
import com.flycms.module.template.model.TemplateAssign;
import com.flycms.module.template.model.TemplateContext;
import com.flycms.module.template.service.AreaBlockService;
import com.flycms.module.template.service.PatternService;
import com.flycms.module.template.service.TagManualService;
import com.flycms.module.template.service.TemplateCenterService;
import com.flycms.module.template.service.TemplateResolver;
import com.flycms.module.template.service.ThemeRegistry;
import com.flycms.module.template.service.ThemeSwitchService;
import com.flycms.module.template.service.WpThemeConverterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板中心 REST（规划 §8 阶段 D）：版本历史/回滚、皮肤 CRUD、皮肤包导入导出、模板试渲染、在线标签手册。
 *
 * 在线编辑的 read/save/create/delete 仍在 ApiWebsiteController（保持既有权限与路径不变），
 * 其 save 已改为走 TemplateCenterService，因此同样享有"语法校验 + 版本快照 + 即时生效"。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiTemplateController extends ApiBaseController {

    @Autowired
    private TemplateCenterService templateCenterService;
    @Autowired
    private TagManualService tagManualService;
    @Autowired
    private com.flycms.module.template.service.TagSuggestService tagSuggestService;
    @Autowired
    private WpThemeConverterService wpThemeConverterService;
    @Autowired
    private ThemeRegistry themeRegistry;
    @Autowired
    private ThemeSwitchService themeSwitchService;
    @Autowired(required = false)
    private TemplateAssignDao templateAssignDao;
    @Autowired
    private TemplateResolver templateResolver;
    @Autowired
    private AreaBlockService areaBlockService;
    @Autowired
    private PatternService patternService;

    /**
     * 指派目标类型白名单（§5.3）。与 {@code fly_template_assign.target_type} 枚举一致：
     * CONTENT=内容、CHANNEL=栏目、MODEL=模型、SITE=站点。
     */
    private static final java.util.Set<String> ASSIGN_TARGET_TYPES =
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("CONTENT", "CHANNEL", "MODEL", "SITE"));

    /**
     * 页面类型白名单。与 {@code TemplateContext.PageType} 一一对应，
     * TAG/ERROR/SEARCH/CHANNEL_PAGE 也支持指派（2026-09-28 补全，此前只认 LIST/DETAIL/INDEX）。
     */
    private static final java.util.Set<String> ASSIGN_PAGE_TYPES =
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    "INDEX", "LIST", "DETAIL", "CHANNEL_PAGE", "SEARCH", "TAG", "ERROR"));

    // /////////////////// D1 版本历史与回滚 ///////////////////

    /** 版本历史（倒序 100 条） */
    @ResponseBody
    @GetMapping("/system/template/versions")
    public DataVo versions(@RequestParam(value = "file") String file,
                           @RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/template/files");
        return DataVo.success("操作成功", templateCenterService.versions(skin(), file));
    }

    /** 回滚：把历史版本作为新版本写入，历史不丢 */
    @ResponseBody
    @PostMapping("/system/template/restore")
    public DataVo restore(@RequestParam("file") String file,
                          @RequestParam("version") int version) {
        requirePermission("/api/system/template/restore");
        Long adminId = currentAdminId();
        return templateCenterService.restore(skin(), file, version, adminId);
    }

    // /////////////////// D8 试渲染 ///////////////////

    /** 不落盘渲染当前编辑器内容，用于"写完立刻知道效果" */
    @ResponseBody
    @PostMapping("/system/template/preview")
    public DataVo preview(@RequestParam(value = "content", required = false) String content,
                          @RequestParam(value = "file", required = false) String file,
                          @RequestParam(value = "model", required = false) String modelCode) {
        requirePermission("/api/system/template/preview");
        Map<String, Object> model = templateCenterService.sampleModel(modelCode);
        if (StringUtils.isNotBlank(file)) {
            model.put("file", file);
        }
        // 相对 include 按模板所在目录解析：试渲染必须用与线上一致的完整加载路径命名
        String templateName = StringUtils.isBlank(file)
                ? null
                : "pc_theme/" + templateCenterService.currentSkin() + "/" + file;
        return templateCenterService.preview(templateName, content, model);
    }

    /** 仅做语法校验（保存前手速检查，不渲染） */
    @ResponseBody
    @PostMapping("/system/template/check")
    public DataVo check(@RequestParam(value = "content", required = false) String content,
                        @RequestParam(value = "file", required = false) String file) {
        requirePermission("/api/system/template/files");
        return templateCenterService.validateSyntax(
                StringUtils.isBlank(file) ? "__check__" : file, content == null ? "" : content);
    }

    // /////////////////// D2/D3 皮肤管理 ///////////////////

    /** 皮肤列表 + 当前皮肤 */
    @ResponseBody
    @GetMapping("/system/skin/list")
    public DataVo skins() {
        requirePermission("/api/system/template/files");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skins", templateCenterService.listSkins());
        data.put("current", templateCenterService.currentSkin());
        return DataVo.success("操作成功", data);
    }

    /** 新建皮肤（可从现有皮肤复制） */
    @ResponseBody
    @PostMapping("/system/skin/save")
    public DataVo saveSkin(@RequestParam("skin") String skin,
                           @RequestParam(value = "from", required = false) String from) {
        requirePermission("/api/system/skin/save");
        return templateCenterService.createSkin(skin, from);
    }

    /**
     * 删除皮肤（拒绝删当前使用皮肤）
     *
     * <p>参数用 {@code required = false} 而非必需：Spring 在<b>方法体之前</b>做参数绑定，
     * 若声明为必需，未登录/漏参的请求会在 {@code requirePermission} 之前就抛
     * 400/500，导致"未登录应得 401"的约定被破坏（2026-09-28 统一）。
     */
    @ResponseBody
    @PostMapping("/system/skin/delete")
    public DataVo deleteSkin(@RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/skin/delete");
        if (StringUtils.isBlank(skin)) {
            return DataVo.failure("请指定要删除的主题");
        }
        return templateCenterService.deleteSkin(skin);
    }

    // /////////////////// D4/D5 皮肤包 ///////////////////

    /** D4 导出皮肤包（zip 下载，含 manifest.json） */
    @GetMapping("/system/skin/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportSkin(
            @RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/skin/export");
        String target = StringUtils.isBlank(skin) ? templateCenterService.currentSkin() : skin;
        byte[] zip;
        try {
            zip = templateCenterService.exportSkin(target);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
        ByteArrayResource resource = new ByteArrayResource(zip);
        String filename = URLEncoder.encode(target + ".zip", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zip.length)
                .body(resource);
    }

    /**
     * D5 导入皮肤包（zip-slip 防御 + 后缀白名单 + 50MB 上限）
     *
     * <p>{@code file} 声明为非必需的原因同 {@link #deleteSkin}：缺 multipart 部件时
     * Spring 的参数绑定会先于鉴权抛 500，未登录就无法得到约定的 401。
     */
    @ResponseBody
    @PostMapping("/system/skin/import")
    public DataVo importSkin(@RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam(value = "overwrite", defaultValue = "0") int overwrite) {
        requirePermission("/api/system/skin/import");
        if (file == null || file.isEmpty()) {
            return DataVo.failure("请选择要导入的主题包（.zip）");
        }
        return templateCenterService.importSkin(file, overwrite == 1);
    }

    // /////////////////// D22 WordPress 主题包转换 + 模板派生 ///////////////////

    /**
     * D22 上传 WordPress 主题包 → 自动转成 FlyCms 皮肤。
     *
     * <p>与 {@code /api/system/skin/import} 的区别：那个导入<b>已经是</b> FlyCms 皮肤包；
     * 这个导入的是<b>原始 WP 主题</b>（含 php），会做 The Loop / 模板函数映射，
     * 并把资源自动落到 {@code assets/skin/pc_theme/{skin}/}，转换完即可切换使用。
     */
    @ResponseBody
    @PostMapping("/system/skin/importWp")
    public DataVo importWp(@RequestParam(value = "file", required = false) MultipartFile file,
                           @RequestParam(value = "skin", required = false) String skin,
                           @RequestParam(value = "overwrite", defaultValue = "0") int overwrite) {
        requirePermission("/api/system/skin/importWp");
        if (file == null || file.isEmpty()) {
            return DataVo.failure("请选择要转换的 WordPress 主题包（.zip）");
        }
        return wpThemeConverterService.convert(file, skin, overwrite == 1);
    }

    /** D22 读取最近一次 WP 转换报告（TODO 清单：还差哪些地方要人工改） */
    @ResponseBody
    @GetMapping("/system/skin/convertReport")
    public DataVo convertReport(@RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/skin/convertReport");
        String targetSkin = StringUtils.isBlank(skin) ? skin() : skin;
        return wpThemeConverterService.report(targetSkin);
    }

    /**
     * D22 模板派生：把已有模板另存为另一个层级槽位名（{@code list-news.html} 等），
     * 用户再在此基础上改。配合模板层级，"给某栏目换版式"只需 1 次新建。
     */
    @ResponseBody
    @PostMapping("/system/template/derive")
    public DataVo derive(@RequestParam("file") String file,
                         @RequestParam("target") String target,
                         @RequestParam(value = "skin", required = false) String skin,
                         @RequestParam(value = "overwrite", defaultValue = "0") int overwrite) {
        requirePermission("/api/system/template/derive");
        String targetSkin = StringUtils.isBlank(skin) ? skin() : skin;
        return templateCenterService.derive(targetSkin, file, target, currentAdminId(), overwrite == 1);
    }

    /** D22 可派生的层级槽位清单（模型/栏目维度生成，标注是否已存在） */
    @ResponseBody
    @GetMapping("/system/template/deriveTargets")
    public DataVo deriveTargets(@RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/template/derive");
        String targetSkin = StringUtils.isBlank(skin) ? skin() : skin;
        return DataVo.success("操作成功", templateCenterService.deriveTargets(targetSkin));
    }

    // /////////////////// P5 主题市场：列表/启用/预览/回滚 ///////////////////

    /** 主题列表（含元信息/缩略图/版本/使用中标记）+ 当前主题 */
    @ResponseBody
    @GetMapping("/system/theme/list")
    public DataVo themeList() {
        requirePermission("/api/system/theme/list");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("themes", themeRegistry.allThemes());
        data.put("current", themeRegistry.currentSkin());
        return DataVo.success("操作成功", data);
    }

    /** 切换前兼容性预检（缺哪些模板、父主题是否缺失） */
    @ResponseBody
    @GetMapping("/system/theme/check")
    public DataVo themeCheck(@RequestParam("code") String code) {
        requirePermission("/api/system/theme/check");
        return themeSwitchService.check(code);
    }

    /** 启用主题：预检 → 原子切换 → 探活 → 失败自动回滚（§6.4 / D19） */
    @ResponseBody
    @PostMapping("/system/theme/enable")
    public DataVo themeEnable(@RequestParam("code") String code) {
        requirePermission("/api/system/theme/enable");
        return themeSwitchService.enable(code, currentAdminId());
    }

    /** 一键回滚到上一主题 */
    @ResponseBody
    @PostMapping("/system/theme/rollback")
    public DataVo themeRollback() {
        requirePermission("/api/system/theme/rollback");
        return themeSwitchService.rollback();
    }

    /** 预览地址（管理员带此参数访问，只影响自己，访客看旧主题） */
    @ResponseBody
    @GetMapping("/system/theme/preview")
    public DataVo themePreview(@RequestParam("code") String code) {
        requirePermission("/api/system/theme/check");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", themeSwitchService.previewUrl(code));
        return DataVo.success("操作成功", data);
    }

    /** 基于父主题创建子主题（声明式 parent，不复制文件，D18） */
    @ResponseBody
    @PostMapping("/system/theme/createChild")
    public DataVo createChild(@RequestParam("child") String child,
                              @RequestParam("parent") String parent,
                              @RequestParam(value = "name", required = false) String name) {
        requirePermission("/api/system/theme/createChild");
        return templateCenterService.createChildSkin(child, parent, name);
    }

    /** 把父主题的指定文件复制到子主题（只复制要改的，D18） */
    @ResponseBody
    @PostMapping("/system/theme/copyParent")
    public DataVo copyParent(@RequestParam("child") String child,
                             @RequestParam("parent") String parent,
                             @RequestParam("files") List<String> files) {
        requirePermission("/api/system/theme/createChild");
        return templateCenterService.copyParentFiles(child, parent, files);
    }

    // /////////////////// P7 模板指派（DB 覆盖层，§5.3） ///////////////////

    /** 指派模板（内容/栏目/模型/站点 → 某页面类型 → 某模板文件） */
    @ResponseBody
    @PostMapping("/system/template/assign")
    public DataVo assign(@RequestParam("targetType") String targetType,
                         @RequestParam("targetId") String targetId,
                         @RequestParam("pageType") String pageType,
                         @RequestParam("template") String template) {
        requirePermission("/api/system/template/assign");
        if (templateAssignDao == null) {
            return DataVo.failure("模板指派模块未启用");
        }
        // 口径校验：这两个枚举是解析器 assignedTemplate() 的唯一查询键，写错就是"指派了但永不命中"的死数据
        String tt = StringUtils.upperCase(StringUtils.trim(targetType));
        if (!ASSIGN_TARGET_TYPES.contains(tt)) {
            return DataVo.failure("未知指派目标类型：" + targetType
                    + "（仅支持 " + ASSIGN_TARGET_TYPES + "）");
        }
        String pt = StringUtils.upperCase(StringUtils.trim(pageType));
        if (!ASSIGN_PAGE_TYPES.contains(pt)) {
            return DataVo.failure("未知页面类型：" + pageType
                    + "（仅支持 " + ASSIGN_PAGE_TYPES + "）");
        }
        if (StringUtils.isBlank(targetId)) {
            return DataVo.failure("指派目标标识不能为空");
        }
        TemplateAssign a = new TemplateAssign();
        a.setTargetType(tt);
        a.setTargetId(StringUtils.trim(targetId));
        a.setPageType(pt);
        a.setTemplate(template.endsWith(".html") ? template : template + ".html");
        templateAssignDao.upsert(a);
        // 切换/指派后主题元信息缓存无需清，但解析结果取决于文件存在性，刷新注册表以便 UI 立即反映
        themeRegistry.refresh();
        return DataVo.success("已指派：" + tt + "/" + targetId + " 的 " + pt + " 页面使用 " + a.getTemplate());
    }

    /** 取消指派 */
    @ResponseBody
    @PostMapping("/system/template/unassign")
    public DataVo unassign(@RequestParam("targetType") String targetType,
                           @RequestParam("targetId") String targetId,
                           @RequestParam("pageType") String pageType) {
        requirePermission("/api/system/template/assign");
        if (templateAssignDao == null) {
            return DataVo.failure("模板指派模块未启用");
        }
        templateAssignDao.deleteByTarget(targetType, targetId, pageType);
        return DataVo.success("已取消指派");
    }

    /** 列举某目标类型的全部指派（后台选择器/审计） */
    @ResponseBody
    @GetMapping("/system/template/assign/list")
    public DataVo assignList(@RequestParam("targetType") String targetType) {
        requirePermission("/api/system/template/assign");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", templateAssignDao == null ? new ArrayList<>() : templateAssignDao.listByTargetType(targetType));
        return DataVo.success("操作成功", data);
    }

    // /////////////////// P9 一键清空模板缓存（§10） ///////////////////

    /** 清空 FreeMarker 全量模板缓存 + 主题注册表缓存；"改了没生效"的最后手段 */
    @ResponseBody
    @PostMapping("/system/template/cache/clear")
    public DataVo clearTemplateCache() {
        requirePermission("/api/system/template/files");
        templateCenterService.clearAllTemplateCache();
        return DataVo.success("模板缓存已清空，下次访问按磁盘当前内容重新加载");
    }

    // /////////////////// P8 候选链调试（§8.2 调试条数据源） ///////////////////

    /**
     * 给定页面上下文，返回完整模板候选链与命中情况（不触发渲染）。
     * pageType: INDEX / LIST / DETAIL / CHANNEL_PAGE / SEARCH / TAG / ERROR
     */
    @ResponseBody
    @GetMapping("/system/template/debugChain")
    public DataVo debugChain(@RequestParam("pageType") String pageType,
                             @RequestParam(value = "model", required = false) String model,
                             @RequestParam(value = "channel", required = false) String channel,
                             @RequestParam(value = "contentId", required = false) Long contentId,
                             @RequestParam(value = "shortUrl", required = false) String shortUrl,
                             @RequestParam(value = "errorCode", required = false) Integer errorCode) {
        requirePermission("/api/system/template/files");
        TemplateContext ctx;
        switch (pageType.toUpperCase()) {
            case "INDEX":
                ctx = TemplateContext.index();
                break;
            case "LIST":
                ctx = TemplateContext.list(model, channel);
                break;
            case "DETAIL":
                ctx = TemplateContext.detail(model, shortUrl, contentId, channel);
                break;
            case "CHANNEL_PAGE":
                ctx = TemplateContext.channelPage(channel);
                break;
            case "SEARCH":
                ctx = TemplateContext.search();
                break;
            case "TAG":
                ctx = TemplateContext.tag(shortUrl);
                break;
            case "ERROR":
                ctx = TemplateContext.error(errorCode == null ? 404 : errorCode);
                break;
            default:
                return DataVo.failure("不支持的页面类型：" + pageType);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", templateResolver.activeSkin());
        data.put("chain", templateResolver.debugChain(ctx));
        return DataVo.success("操作成功", data);
    }

    // /////////////////// D7 在线标签手册 ///////////////////

    /**
     * 标签手册（按分组），后台组件面板/标签手册页数据源。
     *
     * @param scope 可选作用域过滤（§9.2）：{@code global}/{@code list}/{@code detail}/{@code module}。
     *              传了就只保留该作用域 + 全局标签；每行也带 {@code scope}/{@code scopeLabel}，
     *              前端可自行做客户端过滤或分组展示。
     */
    @ResponseBody
    @GetMapping("/system/tags/manual")
    public DataVo manual(@RequestParam(value = "scope", required = false) String scope) {
        requirePermission("/api/system/template/files");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("groups", tagManualService.manual(scope));
        data.put("scopeOptions", tagManualService.scopeOptions());
        data.put("scope", StringUtils.defaultIfBlank(scope, "all"));
        return DataVo.success("操作成功", data);
    }

    // /////////////////// P8 剩余：diff / 智能标签建议 ///////////////////

    /**
     * 版本差异对比（§8.2「差异对比」）。
     * {@code from}/{@code to} 为版本号，传 0 表示「当前磁盘内容」（to）或「空文件」（from）。
     */
    @ResponseBody
    @GetMapping("/system/template/versions/diff")
    public DataVo diff(@RequestParam("file") String file,
                       @RequestParam(value = "skin", required = false) String skin,
                       @RequestParam(value = "from", defaultValue = "0") int from,
                       @RequestParam(value = "to", defaultValue = "0") int to) {
        requirePermission("/api/system/template/files");
        String s = StringUtils.isNotBlank(skin) ? skin : skin();
        return DataVo.success("操作成功", templateCenterService.diffVersions(s, file, from, to));
    }

    /**
     * 智能标签建议（§8.2）：按当前文件名推定模型，给出该模型的字段/筛选字段
     * 与可复制的标签骨架（已带入模型 code 与筛选参数）。
     */
    @ResponseBody
    @GetMapping("/system/template/tagSuggest")
    public DataVo tagSuggest(@RequestParam("file") String file,
                             @RequestParam(value = "pageType", required = false) String pageType) {
        requirePermission("/api/system/template/files");
        return DataVo.success("操作成功", tagSuggestService.suggest(file, pageType));
    }

    // /////////////////// P10 区域编排 V2（§7.3 / §8.4） ///////////////////

    /**
     * 布局编排数据源：主题声明的区域 + 各区域已编排的区块（按区域分组）。
     *
     * @param theme 主题 code，留空取当前主题
     */
    @ResponseBody
    @GetMapping("/system/area/list")
    public DataVo areaList(@RequestParam(value = "theme", required = false) String theme) {
        requirePermission("/api/system/area/list");
        String code = StringUtils.isNotBlank(theme) ? theme : templateRegistryCurrentSkin();
        List<com.flycms.module.template.model.AreaBlock> blocks = areaBlockService.listByTheme(code);
        Map<String, List<com.flycms.module.template.model.AreaBlock>> grouped = new LinkedHashMap<>();
        for (com.flycms.module.template.model.AreaBlock b : blocks) {
            grouped.computeIfAbsent(b.getAreaName(), k -> new ArrayList<>()).add(b);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("theme", code);
        data.put("regions", areaBlockService.regions(code));
        data.put("blocks", grouped);
        return DataVo.success("操作成功", data);
    }

    /** 新增区块 */
    @ResponseBody
    @PostMapping("/system/area/save")
    public DataVo areaSave(@RequestParam(value = "id", required = false) Long id,
                           @RequestParam(value = "theme", required = false) String theme,
                           @RequestParam("areaName") String areaName,
                           @RequestParam("blockType") String blockType,
                           @RequestParam(value = "blockTitle", required = false) String blockTitle,
                           @RequestParam("blockRef") String blockRef,
                           @RequestParam(value = "sort", required = false) Integer sort,
                           @RequestParam(value = "status", required = false) Integer status) {
        requirePermission("/api/system/area/save");
        com.flycms.module.template.model.AreaBlock b = new com.flycms.module.template.model.AreaBlock();
        b.setId(id);
        b.setThemeCode(StringUtils.isNotBlank(theme) ? theme : templateRegistryCurrentSkin());
        b.setAreaName(StringUtils.trim(areaName));
        b.setBlockType(StringUtils.upperCase(StringUtils.trim(blockType)));
        b.setBlockTitle(blockTitle);
        b.setBlockRef(blockRef);
        b.setSort(sort);
        b.setStatus(status);
        return id == null ? areaBlockService.add(b) : areaBlockService.update(b);
    }

    /** 删除区块 */
    @ResponseBody
    @PostMapping("/system/area/delete")
    public DataVo areaDelete(@RequestParam("id") Long id) {
        requirePermission("/api/system/area/save");
        return areaBlockService.delete(id);
    }

    /** 批量重排（拖拽后提交一次） */
    @ResponseBody
    @PostMapping("/system/area/reorder")
    public DataVo areaReorder(@RequestParam("ids") String ids) {
        requirePermission("/api/system/area/save");
        List<Long> list = new ArrayList<>();
        for (String s : StringUtils.split(StringUtils.defaultString(ids), ',')) {
            try {
                list.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法 id
            }
        }
        return areaBlockService.reorder(list);
    }

    /** 启用/停用区块 */
    @ResponseBody
    @PostMapping("/system/area/status")
    public DataVo areaStatus(@RequestParam("id") Long id,
                             @RequestParam(value = "status", defaultValue = "1") Integer status) {
        requirePermission("/api/system/area/save");
        return areaBlockService.toggleStatus(id, status);
    }

    /** 区域渲染预览（保存后立刻看效果） */
    @ResponseBody
    @GetMapping("/system/area/preview")
    public DataVo areaPreview(@RequestParam(value = "theme", required = false) String theme,
                              @RequestParam("area") String area) {
        requirePermission("/api/system/area/list");
        String code = StringUtils.isNotBlank(theme) ? theme : templateRegistryCurrentSkin();
        return areaBlockService.preview(code, area);
    }

    /** 当前主题 code（与 ThemeRegistry.currentSkin 一致，避免各处重复取配置） */
    private String templateRegistryCurrentSkin() {
        return themeRegistry.currentSkin();
    }

    // /////////////////// P-4 区块化编辑器：页面-区域映射 ///////////////////

    /**
     * 区域使用映射：扫描当前皮肤全部模板，解析 {@code <@fly_area name="xxx">} 引用，
     * 返回 区域名 → 引用它的模板文件列表。建站者据此知道"改哪个区域生效在哪个页面"。
     * 结果实时扫描（模板数量有限，页面打开时一次调用可接受）。
     */
    @ResponseBody
    @GetMapping("/system/area/usage")
    public DataVo areaUsage(@RequestParam(value = "theme", required = false) String theme) {
        requirePermission("/api/system/area/list");
        String skin = StringUtils.isNotBlank(theme) ? theme : templateRegistryCurrentSkin();
        java.io.File root = new java.io.File("views/templates/pc_theme/" + skin);
        Map<String, List<String>> usage = new LinkedHashMap<>();
        if (root.exists() && root.isDirectory()) {
            java.util.Deque<java.io.File> stack = new java.util.ArrayDeque<>();
            stack.push(root);
            while (!stack.isEmpty()) {
                java.io.File dir = stack.pop();
                java.io.File[] children = dir.listFiles();
                if (children == null) {
                    continue;
                }
                for (java.io.File f : children) {
                    if (f.isDirectory()) {
                        stack.push(f);
                    } else if (f.getName().endsWith(".html")) {
                        try {
                            String text = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                                    java.nio.charset.StandardCharsets.UTF_8);
                            java.util.regex.Matcher m = java.util.regex.Pattern
                                    .compile("<@fly_area\s+name=\"([A-Za-z0-9_-]+)\"")
                                    .matcher(text);
                            String rel = f.getPath().replace("views\\templates\\pc_theme\\", "")
                                    .replace('\\', '/');
                            while (m.find()) {
                                usage.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(rel);
                            }
                        } catch (Exception ignored) {
                            // 单文件读取失败不影响整体
                        }
                    }
                }
            }
        }
        return DataVo.success("操作成功", usage);
    }


    // /////////////////// P3-3 区块图案（§7.1 / §8.2 组件面板） ///////////////////

    /**
     * 图案库清单（只读展示 + 一键插入的数据源）。
     * 图案是"想加一块内容时抄一段"的素材，不参与模板解析链。
     */
    @ResponseBody
    @GetMapping("/system/pattern/list")
    public DataVo patternList(@RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/template/files");
        String s = StringUtils.isNotBlank(skin) ? skin : skin();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", s);
        data.put("patterns", patternService.list(s));
        return DataVo.success("操作成功", data);
    }

    /** 读取图案内容（编辑器插入用） */
    @ResponseBody
    /** V5 模板片段库：保存自定义图案（同名覆盖；content 建议先过 /system/template/preview 校验） */
    @PostMapping("/system/pattern/save")
    public DataVo patternSave(@RequestParam(value = "skin", required = false) String skin,
                              @RequestParam("name") String name,
                              @RequestParam("content") String content) {
        requirePermission("/api/system/template/save");
        String s = skin == null || skin.isBlank() ? templateResolver.activeSkin() : skin;
        return patternService.save(s, name, content);
    }

    /** V5 模板片段库：删除图案 */
    @ResponseBody
    @PostMapping("/system/pattern/delete")
    public DataVo patternDelete(@RequestParam(value = "skin", required = false) String skin,
                                @RequestParam("name") String name) {
        requirePermission("/api/system/template/save");
        String s = skin == null || skin.isBlank() ? templateResolver.activeSkin() : skin;
        return patternService.delete(s, name);
    }

    @ResponseBody
    @GetMapping("/system/pattern/read")
    public DataVo patternRead(@RequestParam("file") String file,
                              @RequestParam(value = "skin", required = false) String skin) {
        requirePermission("/api/system/template/files");
        String s = StringUtils.isNotBlank(skin) ? skin : skin();
        return patternService.read(s, file);
    }

    // /////////////////// 内部 ///////////////////

    private String skin() {
        return templateCenterService.currentSkin();
    }

    private Long currentAdminId() {
        com.flycms.module.admin.model.Admin admin = AdminSessionUtils.getLoginMember(request);
        return admin == null ? null : admin.getId();
    }
}
