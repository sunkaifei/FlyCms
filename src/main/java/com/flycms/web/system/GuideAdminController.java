package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.config.model.Guide;
import com.flycms.module.config.service.GuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 18:47 2018/7/19
 */
@Controller
@RequestMapping("/system/guide")
public class GuideAdminController extends BaseController{
    @Autowired
    protected GuideService guideService;

    //按父级id查询所属的所有分类列表
    @GetMapping(value = "/list_guide")
    public String getCategoryList(@RequestParam(value = "id", defaultValue = "0") int id, ModelMap modelMap){

        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/list_guide");
    }

    //添加积分规则
    @GetMapping(value = "/add_guide")
    public String addScoreRule(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/add_guide");
    }

    //增加新积分规则信息
    @PostMapping("/guide_save")
    @ResponseBody
    public DataVo addScoreRule(@Valid Guide guide, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            if(guide.getName()==null){
                return data=DataVo.failure("规则名称不能为空！");
            }
            if(guide.getLink()==null){
                return data=DataVo.failure("请设置每次变动的积分！");
            }
            data = guideService.addGuide(guide);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
