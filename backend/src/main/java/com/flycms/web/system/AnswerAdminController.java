package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.question.model.Answer;
import com.flycms.module.question.service.AnswerService;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:09 2018/9/3
 */
@Controller
@RequestMapping("/system/answer")
public class AnswerAdminController extends BaseController {
    @Autowired
    private AnswerService answerService;

    @GetMapping(value = "/list_answer")
    public String answerList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_answer");
    }

    //查询单条回答数据信息
    @ResponseBody
    @GetMapping(value = "/findId")
    public DataVo getFindAnswerId(@RequestParam(value = "id", required = false) String id, ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data = DataVo.failure("id参数错误");
            }
            Answer answer=answerService.findAnswerById(Long.parseLong(id), 0);
            if(answer==null) {
                return DataVo.failure("id错误或不存在！");
            }else {
                return DataVo.success("查询成功", answer);
            }
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //答案审核操作
    @PostMapping("/answer-status")
    @ResponseBody
    public DataVo updateAnswerStatusById(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "status", required = false) String status) throws Exception{
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        if (!NumberUtils.isNumber(status)) {
            return data = DataVo.failure("审核状态参数错误");
        }
        data = answerService.updateAnswerStatusById(Long.parseLong(id),Integer.valueOf(status));
        return data;
    }

    //删除权限组
    @PostMapping("/del")
    @ResponseBody
    public DataVo deleteAnswerById(@RequestParam(value = "id") String id){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data=DataVo.failure("话题参数错误");
        }
        data = answerService.deleteAnswerById(Long.parseLong(id));
        return data;
    }
}
