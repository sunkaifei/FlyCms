package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelField;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelFieldService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.template.service.ThemeRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 前台投稿通道（U3 收口：旧 /ucenter/article/article_save 随文章模块退役，投稿按模型重建）。
 *
 * <p>万能模型口径：任意启用模型皆可开放前台投稿——
 * <ul>
 *   <li>会话用户投稿（UserInterceptor 守卫，需 fly_user_permission 行 {@code /ucenter/submit/*} 授权）；</li>
 *   <li>字段白名单 = 启用且 is_form=1 的自定义字段 + 固有列（title/categoryId/thumbnail/keywords/description/content/publish_time），
 *       表单隐藏字段、未知键一律丢弃；status 不可由前台指定；</li>
 *   <li>状态由审核开关决定：config 键 {@code fly_article_audit}=1 → 落 0（待审），0 → 落 1（直接发布）；</li>
 *   <li>落库走 ModelDataService.insertData（字段校验/唯一约束/附件引用计数/G12 版本快照全继承）。</li>
 * </ul>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class SubmitController extends BaseController {

    /** 固有列白名单（前台投稿可提交的主表列） */
    private static final Set<String> FIXED_KEYS = Set.of(
            "title", "categoryId", "thumbnail", "keywords", "description", "content", "publish_time");

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelFieldService modelFieldService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private com.flycms.module.model.service.ModelCategoryService modelCategoryService;
    @Autowired
    private com.flycms.module.user.service.UserService userService;
    @Autowired
    private ThemeRegistry themeRegistry;

    /**
     * V2 投稿表单页：渲染 submit-{code}.html（缺省回退 submit.html），
     * 表单字段由 formMeta 驱动（模板内循环 meta.fields 按类型渲染）。
     * 模型未开放投稿（enable_submit=0）或未登录一律 404/登录语义拒绝。
     */
    @GetMapping("/ucenter/submit/{modelCode}")
    public String submitPage(@PathVariable String modelCode,
                             @RequestParam Map<String, String> params,
                             ModelMap modelMap) {
        if (getUser() == null) {
            return "redirect:/login";
        }
        final String code;
        try {
            code = SqlSafeUtil.safeModelCode(modelCode);
        } catch (IllegalArgumentException e) {
            return theme.getPcTemplate("404");
        }
        Model model = modelService.findModelByCode(code);
        if (model == null || model.getStatus() != 1
                || (model.getEnableSubmit() != null && model.getEnableSubmit() == 0)) {
            return theme.getPcTemplate("404");
        }
        String skin = themeRegistry.currentSkin();
        ThemeRegistry.Resolved r = themeRegistry.locate(skin, "submit-" + code);
        if (r == null) {
            r = themeRegistry.locate(skin, "submit");
        }
        if (r == null) {
            // 主题未提供投稿模板：提示信息页（避免裸 500）
            modelMap.addAttribute("message", "该模型未开放前台投稿页面（缺少 submit 模板）");
            return theme.getPcTemplate("message_tip");
        }
        modelMap.addAttribute("model", model);
        Map<String, Object> meta = modelDataService.formMeta(model.getId());
        // X1 投稿上下文预置：URL 白名单参数（id 类字段）校验后转只读上下文条
        List<Map<String, Object>> fixed;
        try {
            fixed = resolveFixed(model, params);
        } catch (IllegalArgumentException e) {
            modelMap.addAttribute("message", e.getMessage());
            return theme.getPcTemplate("message_tip");
        }
        // 模板友好化：fields 转.Map 并解析 options → optionsList（radio/checkbox 直接渲染）
        if (meta.get("fields") instanceof List) {
            List<Map<String, Object>> fieldMaps = new ArrayList<>();
            Set<String> fixedNames = new HashSet<>();
            for (Map<String, Object> f : fixed) {
                fixedNames.add(String.valueOf(f.get("name")));
            }
            for (ModelField f : (List<ModelField>) meta.get("fields")) {
                // 已作上下文预置的字段不再进可编辑字段循环（由 fixed 条 + hidden 呈现）
                if (fixedNames.contains(f.getFieldName())) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("fieldName", f.getFieldName());
                m.put("fieldLabel", f.getFieldLabel());
                m.put("fieldType", f.getFieldType());
                m.put("isRequired", f.getIsRequired());
                m.put("isForm", f.getIsForm());
                m.put("placeholder", f.getPlaceholder());
                List<String> opts = new ArrayList<>();
                if (StringUtils.isNotBlank(f.getOptions())) {
                    try {
                        opts = com.alibaba.fastjson2.JSON.parseArray(f.getOptions(), String.class);
                    } catch (Exception e) {
                        for (String o : f.getOptions().split(",")) {
                            opts.add(StringUtils.strip(o, "[]"));
                        }
                    }
                }
                m.put("optionsList", opts);
                fieldMaps.add(m);
            }
            meta.put("fields", fieldMaps);
        }
        meta.put("fixed", fixed);
        modelMap.addAttribute("meta", meta);
        modelMap.addAttribute("user", getUser());
        return r.view;
    }

    @ResponseBody
    @PostMapping("/ucenter/submit/{modelCode}")
    public DataVo submit(@PathVariable String modelCode,
                         @RequestParam Map<String, String> form) {
        if (getUser() == null) {
            return DataVo.failure("请登录后投稿");
        }
        final String code;
        try {
            code = SqlSafeUtil.safeModelCode(modelCode);
        } catch (IllegalArgumentException e) {
            return DataVo.failure("模型标识不合法");
        }
        Model model = modelService.findModelByCode(code);
        if (model == null || model.getStatus() != 1
                || (model.getEnableSubmit() != null && model.getEnableSubmit() == 0)) {
            return DataVo.failure("模型不存在或未开放投稿");
        }

        // 字段白名单：启用 + 表单可见的自定义字段（is_form=0 条件显隐未命中的语义相同：不开放提交）
        List<ModelField> fields = modelFieldService.findFieldsByModelId(model.getId(), 1);
        Set<String> allowed = new HashSet<>(FIXED_KEYS);
        for (ModelField f : fields) {
            if (f.getIsForm() != 0) {
                allowed.add(f.getFieldName());
            }
        }
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (allowed.contains(e.getKey()) && StringUtils.isNotBlank(e.getValue())) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        if (StringUtils.isBlank(filtered.get("title"))) {
            return DataVo.failure(model.getTitleLabel() + "不能为空");
        }

        // X1 投稿上下文：白名单 id 类参数校验存在性后强制写入（服务端为唯一事实源，
        // 存在性/发布态校验在 resolveFixed + insertData 双保险）
        try {
            for (Map<String, Object> f : resolveFixed(model, form)) {
                filtered.put(String.valueOf(f.get("name")), String.valueOf(f.get("value")));
            }
        } catch (IllegalArgumentException e) {
            return DataVo.failure(e.getMessage());
        }

        // 状态不由前台指定：审核开关（0=直接发布 1=先审后发）
        filtered.put("status", configService.getIntKey("fly_article_audit", 0) == 1 ? "0" : "1");

        // userId=内容归属（user_id 列）；editorId=版本快照操作人（同为投稿人）
        Long uid = getUser().getUserId();
        return modelDataService.insertData(model.getId(), filtered, uid, uid);
    }

    // /////////////////// X2 内容所有者操作（采纳/编辑/下架自己的内容） ///////////////////

    /**
     * 所有者更新：行 user_id=当前用户 时可更新该模型 is_form=1 的非关联自定义字段
     * （relate 关联边不可改，status 固有列不可经此通道修改——不绕审核流）。
     * 表单可带 redirect（同站路径）供零 JS 模板表单 302 回详情页；否则返回 JSON。
     */
    @PostMapping("/ucenter/content/update")
    public void ownerUpdate(@RequestParam Map<String, String> form,
                            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        DataVo vo;
        try {
            if (getUser() == null) {
                vo = DataVo.failure("请登录后操作");
            } else {
                String code = null;
                try {
                    code = SqlSafeUtil.safeModelCode(form.get("modelCode"));
                } catch (IllegalArgumentException ignored) {
                    // 非法标识按参数错误处理
                }
                Model model = code == null ? null : modelService.findModelByCode(code);
                Long id = parseLongSafely(form.get("id"));
                if (model == null || id == null) {
                    vo = DataVo.failure("参数传递错误");
                } else {
                    vo = modelDataService.ownerUpdate(model.getId(), id, form, getUser().getUserId());
                }
            }
        } catch (Exception e) {
            vo = DataVo.failure("操作失败");
        }
        String redirect = StringUtils.trimToEmpty(form.get("redirect"));
        if (redirect.startsWith("/") && !redirect.startsWith("//")) {
            response.sendRedirect(redirect + (vo.getCode() == DataVo.CODE_SUCCESS ? "?ok=1" : "?err=1"));
            return;
        }
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(com.alibaba.fastjson2.JSON.toJSONString(vo));
    }

    // /////////////////// X1 上下文预置解析 ///////////////////

    /**
     * 解析并校验投稿上下文预置参数：仅 id 类字段（is_form=1 的 relate/user/category
     * + 固有 categoryId）可预置，值必须通过存在性/归属/发布态校验（校验失败抛
     * IllegalArgumentException，消息面向用户）。
     *
     * @return fixed 列表：{name, label, value, display}（display 供只读条展示）
     */
    private List<Map<String, Object>> resolveFixed(Model model, Map<String, String> params) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<ModelField> fields = modelFieldService.findFieldsByModelId(model.getId(), 1);
        List<String> candidates = new ArrayList<>();
        candidates.add("categoryId");
        for (ModelField f : fields) {
            if (f.getIsForm() == 0) {
                continue;
            }
            String t = f.getFieldType();
            if ("relate".equals(t) || "user".equals(t) || "category".equals(t)) {
                candidates.add(f.getFieldName());
            }
        }
        for (String name : candidates) {
            String raw = params.get(name);
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            Long id = parseLongSafely(raw);
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            if (id == null) {
                throw new IllegalArgumentException("参数不合法：" + name);
            }
            if ("categoryId".equals(name)) {
                var cat = modelCategoryService.findCategoryById(id);
                if (cat == null || !String.valueOf(cat.getModelId()).equals(String.valueOf(model.getId()))) {
                    throw new IllegalArgumentException("所选分类不存在或不属于该模型");
                }
                item.put("label", "分类");
                item.put("value", id);
                item.put("display", cat.getName());
            } else {
                ModelField field = fields.stream()
                        .filter(f -> f.getFieldName().equals(name))
                        .findFirst().orElse(null);
                if (field == null) {
                    continue;
                }
                String type = field.getFieldType();
                if ("relate".equals(type)) {
                    String target = StringUtils.defaultIfBlank(field.getRelateModel(), model.getCode());
                    Map<String, Object> row = modelDataService.findPublishedRow(target, id);
                    if (row == null) {
                        throw new IllegalArgumentException(field.getFieldLabel() + "不存在或未发布");
                    }
                    item.put("label", field.getFieldLabel());
                    item.put("value", id);
                    item.put("display", StringUtils.defaultIfBlank(String.valueOf(row.get("title")), raw));
                    item.put("shortUrl", StringUtils.trimToEmpty(String.valueOf(row.get("shortUrl"))));
                } else if ("user".equals(type)) {
                    var user = userService.findUserById(id, 0);
                    if (user == null) {
                        throw new IllegalArgumentException(field.getFieldLabel() + "对应的用户不存在");
                    }
                    item.put("label", field.getFieldLabel());
                    item.put("value", id);
                    item.put("display", StringUtils.defaultIfBlank(user.getNickName(), user.getUserName()));
                } else {
                    String bound = StringUtils.defaultIfBlank(field.getRelateModel(), model.getCode());
                    var boundModel = modelService.findModelByCode(bound);
                    var cat = modelCategoryService.findCategoryById(id);
                    if (cat == null || boundModel == null
                            || !String.valueOf(cat.getModelId()).equals(String.valueOf(boundModel.getId()))) {
                        throw new IllegalArgumentException(field.getFieldLabel() + "所选分类不存在或不属于绑定模型");
                    }
                    item.put("label", field.getFieldLabel());
                    item.put("value", id);
                    item.put("display", cat.getName());
                }
            }
            out.add(item);
        }
        return out;
    }

    private Long parseLongSafely(String raw) {
        try {
            return Long.parseLong(StringUtils.trimToEmpty(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
