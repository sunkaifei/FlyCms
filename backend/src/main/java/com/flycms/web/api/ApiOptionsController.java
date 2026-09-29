package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.service.ModelCategoryService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用数据源选项 REST（B1 字段绑定数据源）。
 *
 * <p>为「绑定平台数据源」类字段提供表单候选项：
 * <ul>
 *   <li>{@code user} 字段 → {@code /system/options/users}：可搜索用户下拉（显示昵称，提交存 user_id）</li>
 *   <li>{@code category} 字段 → {@code /system/options/categories}：绑定模型的分类树
 *       （树下拉，提交存分类 id；modelCode 留空 = 当前模型的分类树）</li>
 * </ul>
 * 管理端表单使用，走 requirePermission 权限闭环（上线需 permission_sync + 角色组勾选）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiOptionsController extends ApiBaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelCategoryService modelCategoryService;

    /**
     * 用户选项（可搜索）：[{id: "用户id", label: "昵称（用户名）"}]
     */
    @ResponseBody
    @GetMapping("/system/options/users")
    public DataVo users(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "p", defaultValue = "1") int pageNum,
                        @RequestParam(value = "rows", defaultValue = "50") int rows) {
        requirePermission("/api/system/options/users");
        List<User> list = userService.getUserOptions(keyword, Math.min(Math.max(rows, 1), 200));
        List<Map<String, Object>> options = new ArrayList<>();
        for (User u : list) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", String.valueOf(u.getUserId()));
            String nick = u.getNickName();
            String label = StringUtils.isNotBlank(nick) ? nick : u.getUserName();
            if (!StringUtils.equals(label, u.getUserName())) {
                label = label + "（" + u.getUserName() + "）";
            }
            o.put("label", label);
            options.add(o);
        }
        return DataVo.success("操作成功", options);
    }

    /**
     * 分类选项（绑定模型的分类树平铺，前端组树）：[{id, name, fatherId, status}]
     */
    @ResponseBody
    @GetMapping("/system/options/categories")
    public DataVo categories(@RequestParam(value = "modelCode", required = false) String modelCode) {
        requirePermission("/api/system/options/categories");
        if (StringUtils.isBlank(modelCode)) {
            // 本模型分类树由 formMeta 直接随表单元数据下发，这里只服务「绑定其他模型」的场景
            return DataVo.failure("请传入 modelCode");
        }
        String code = modelCode;
        final Model model;
        try {
            model = modelService.findModelByCode(SqlSafeUtil.safeModelCode(code));
        } catch (IllegalArgumentException e) {
            return DataVo.failure("模型标识不合法");
        }
        if (model == null) {
            return DataVo.failure("绑定的模型不存在");
        }
        List<ModelCategory> list = modelCategoryService.findCategoriesByModelId(model.getId(), null);
        List<Map<String, Object>> options = new ArrayList<>();
        for (ModelCategory c : list) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", String.valueOf(c.getId()));
            o.put("name", c.getName());
            o.put("fatherId", String.valueOf(c.getFatherId()));
            o.put("status", c.getStatus());
            options.add(o);
        }
        return DataVo.success("操作成功", options);
    }
}
