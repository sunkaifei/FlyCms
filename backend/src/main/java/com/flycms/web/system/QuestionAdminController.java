package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.service.QuestionService;
import com.flycms.module.search.service.SolrService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-7-2
 */
@Controller
@RequestMapping("/system/question")
public class QuestionAdminController extends BaseController {
    @Autowired
    protected QuestionService questionService;
    @Autowired
    private SolrService solrService;

    @GetMapping(value = "/list_question")
    public String questionList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_question");
    }

    //查询单条问题数据信息
    @ResponseBody
    @GetMapping(value = "/findId")
    public DataVo getFindQuestionId(@RequestParam(value = "id", required = false) String id, ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data = DataVo.failure("id参数错误");
            }
            Question question=questionService.findQuestionById(Long.parseLong(id),0);
            if(question==null) {
                return DataVo.failure("id错误或不存在！");
            }else {
                return DataVo.success("查询成功", question);
            }
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //问题审核操作
    @PostMapping("/question-status")
    @ResponseBody
    public DataVo updateQuestionStatusById(@RequestParam(value = "id", required = false) String id,
                                           @RequestParam(value = "status", required = false) String status,
                                           @RequestParam(value = "recommend", defaultValue = "0") String recommend) throws Exception{
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        if (!NumberUtils.isNumber(status)) {
            return data = DataVo.failure("审核状态参数错误");
        }
        if (!StringUtils.isBlank(recommend)) {
            if (!NumberUtils.isNumber(recommend)) {
                return data = DataVo.failure("推荐参数错误");
            }
        }
        data = questionService.updateQuestionStatusById(Long.parseLong(id),Integer.valueOf(status),Integer.valueOf(recommend));
        return data;
    }

    //编辑问题内容
    @GetMapping(value = "/edit_question/{id}")
    public String editQuestion(@PathVariable String id,ModelMap modelMap){
        Question question=questionService.findQuestionById(Long.parseLong(id),0);
        modelMap.addAttribute("admin", getAdminUser());
        modelMap.addAttribute("question", question);
        return theme.getAdminTemplate("content/edit_question");
    }

    //删除问题
    @PostMapping("/del")
    @ResponseBody
    public DataVo deleteQuestionById(@RequestParam(value = "id", required = false) String id){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        data = questionService.deleteQuestionById(Long.parseLong(id));
        return data;
    }

    //索引所有问答信息
    @ResponseBody
    @RequestMapping(value = "/index_all_question")
    public DataVo indexAllShare() {
        DataVo data = DataVo.failure("操作失败");
        try {
            solrService.indexAllQuestion();
            data=DataVo.success("全部索引成功！");
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
