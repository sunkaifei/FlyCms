package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.score.model.ScoreRule;
import com.flycms.module.score.service.ScoreRuleService;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 20:19 2018/8/31
 */
@Controller
@RequestMapping("/system/score")
public class ScoreAdminController extends BaseController {
    @Autowired
    protected ScoreRuleService scoreRuleService;
    //规则列表
    @GetMapping(value = "/list_scorerule")
    public String ruleList(@RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "createTime", required = false) String createTime,
                           @RequestParam(value = "status", required = false) Integer status,
                           @RequestParam(value = "p", defaultValue = "1") int p,
                           ModelMap modelMap) throws Exception {
        modelMap.put("name", name);
        modelMap.put("createTime", createTime);
        modelMap.put("status", status);
        modelMap.put("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/list_score_rule");
    }

    //添加积分规则
    @GetMapping(value = "/add_scorerule")
    public String addScoreRule(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/add_scorerule");
    }

    //增加新积分规则信息
    @PostMapping("/scorerule_save")
    @ResponseBody
    public DataVo addScoreRule(@Valid ScoreRule scoreRule, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            if(scoreRule.getName()==null){
                return data=DataVo.failure("规则名称不能为空！");
            }
            if(scoreRule.getScore()==null){
                return data=DataVo.failure("请设置每次变动的积分！");
            }
            if(scoreRule.getRemark()==null){
                return data=DataVo.failure("请填写积分变动说明");
            }
            if(scoreRule.getType()==null){
                return data=DataVo.failure("请选择增加积分次数");
            }
            data = scoreRuleService.addScoreRule(scoreRule);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //处理用户组信息
    @PostMapping("/rule_status")
    @ResponseBody
    public DataVo updateRuleStatus(@RequestParam(value = "id", required = false) String id){
        if (!NumberUtils.isNumber(id)) {
            return DataVo.failure("参数传递错误");
        }
        return scoreRuleService.updateRuleStatus(Long.parseLong(id));
    }

    //添加积分规则
    @GetMapping(value = "/update_scorerule/{id}")
    public String updateScoreRule(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        ScoreRule rule=scoreRuleService.findScoreRuleById(Long.parseLong(id),0);
        if(rule==null){
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("rule", rule);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/update_scorerule");
    }

    //增加新积分规则信息
    @PostMapping("/scorerule_update")
    @ResponseBody
    public DataVo updateScoreRule(@Valid ScoreRule scoreRule, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            if(scoreRule.getName()==null){
                return data=DataVo.failure("规则名称不能为空！");
            }
            if(scoreRule.getScore()==null){
                return data=DataVo.failure("请设置每次变动的积分！");
            }
            if(scoreRule.getRemark()==null){
                return data=DataVo.failure("请填写积分变动说明");
            }
            if(scoreRule.getType()==null){
                return data=DataVo.failure("请选择增加积分次数");
            }
            data = scoreRuleService.updateScoreRule(scoreRule);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //删除权限组
    @PostMapping("/del")
    @ResponseBody
    public DataVo deleteRole(@RequestParam(value = "id") int id){
        DataVo data = DataVo.failure("操作失败");
        if(id==1){
            return data = DataVo.failure("超级管理员组不能删除");
        }
        data = scoreRuleService.deleteScoreRuleById(id);
        return data;
    }

    //规则列表
    @GetMapping(value = "/detail_list")
    public String detailList(@RequestParam(value = "name", required = false) String name,
                           @RequestParam(value = "createTime", required = false) String createTime,
                           @RequestParam(value = "status", required = false) Integer status,
                           @RequestParam(value = "p", defaultValue = "1") int p,
                           ModelMap modelMap) throws Exception {
        modelMap.put("name", name);
        modelMap.put("createTime", createTime);
        modelMap.put("status", status);
        modelMap.put("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/list_score_detail");
    }
}
