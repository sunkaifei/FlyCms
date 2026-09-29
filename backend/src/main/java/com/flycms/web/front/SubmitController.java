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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
        if (model == null || model.getStatus() != 1) {
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

        // 状态不由前台指定：审核开关（0=直接发布 1=先审后发）
        filtered.put("status", configService.getIntKey("fly_article_audit", 0) == 1 ? "0" : "1");

        // userId=内容归属（user_id 列）；editorId=版本快照操作人（同为投稿人）
        Long uid = getUser().getUserId();
        return modelDataService.insertData(model.getId(), filtered, uid, uid);
    }
}
