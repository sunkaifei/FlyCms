package com.flycms.web.front;

import com.flycms.core.utils.StringHelperUtils;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.question.model.Answer;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.model.QuestionCount;
import com.flycms.module.question.service.AnswerService;
import com.flycms.module.question.service.QuestionService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class QuestionController extends BaseController {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    /**
     * 问答首页
     *
     * @return
     */
    @GetMapping(value = {"/qc/" , "/qc/index"})
    public String indexShare(@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("question/index");
    }

    //问答详细页面
    @GetMapping(value = "/q/{shortUrl}")
    public String question(@PathVariable(value = "shortUrl", required = false) String shortUrl,@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        Question question=questionService.findQuestionByShorturl(shortUrl);
        if(question==null){
            return theme.getPcTemplate("404");
        }
        //查询统计信息
        QuestionCount count=questionService.findQuestionCountById(question.getId());
        question.setCountAnswer(count.getCountAnswer());
        question.setCountView(count.getCountView());
        question.setCountFollow(count.getCountFollow());
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
            //检查登陆后是否已关注该问题
            Boolean questionFollow=questionService.checkQuestionFollow(question.getId(),getUser().getUserId());
            modelMap.addAttribute("questionFollow", questionFollow);
        }
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("question", question);
        return theme.getPcTemplate("question/detail");
    }

    //问答详细页面
    @ResponseBody
    @RequestMapping(value = "/findQuestionById/{id}")
    public DataVo findQuestionById(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        Map<String, Object> map = new HashMap<>();
        Question question=questionService.findQuestionById(Long.parseLong(id),2);
        if(question==null){
            return data = DataVo.failure("该内容不存在或者未审核！");
        }else{
            //没有答案的时候直接读取问题内容，有回答的时候读取最新回答的答案内容
            if(question.getCountAnswer()==0){
                map.put("content",question.getContent());
            }else{
                Answer answer = answerService.findNewestAnswerById(Long.parseLong(id));
                map.put("content",answer.getContent());
            }
        }
        return DataVo.success(map);
    }

    //发布问题
    @GetMapping(value = "/question/add")
    public String addQuestion(ModelMap modelMap){
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        return theme.getPcTemplate("question/add");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/question/add")
    public DataVo addQuestion(@RequestParam(value = "title", required = false) String title,
                             @RequestParam(value = "content", required = false) String content,
                             @RequestParam(value = "tags", required = false) String tags,
                              ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            title=StringHelperUtils.htmlReplace(title);
            if (StringUtils.isBlank(title)) {
                return data = DataVo.failure("标题不能为空");
            }
            if (StringUtils.isBlank(tags)) {
                return data = DataVo.failure("话题不能为空");
            }
            if(questionService.checkQuestionByTitle(title,getUser().getUserId())){
                return data = DataVo.failure("请勿发布重复内容！");
            }
            title=StringHelperUtils.htmlReplace(title);
            tags=StringHelperUtils.htmlReplace(tags);
            data=questionService.addQuestion(title,content,tags,getUser().getUserId());
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑问题
    @GetMapping(value = "/ucenter/question/edit")
    public String editQuestion(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        Question question=questionService.findQuestionById(Long.parseLong(id),0);
        if (question == null) {
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("question", question);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("question/edit");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/answer/add")
    public DataVo addAnswer(@RequestParam(value = "questionId", required = false) String questionId,
                            @RequestParam(value = "content", required = false) String content,
                              ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(questionId)) {
                return data=DataVo.failure("话题参数错误");
            }
            Question question=questionService.findQuestionById(Long.parseLong(questionId),2);
            if(question==null){
                return data=DataVo.failure("该话题不存在或已删除");
            }
            //content=StringHelperUtil.htmlReplace(content);
            if (StringUtils.isBlank(content)) {
                return DataVo.failure("答案内容不能为空");
            }
            data=answerService.addAnswer(Long.parseLong(questionId),getUser().getUserId(),content);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑问题
    @GetMapping(value = "/ucenter/answer/edit-{id}")
    public String editAnswer(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }

        Answer answer=answerService.findAnswerByIdAndUserId(Long.parseLong(id),getUser().getUserId());
        if (answer == null) {
            return theme.getPcTemplate("404");
        }
        answer.setContent(StringHelperUtils.htmlspecialchars(answer.getContent()));
        Question question=questionService.findQuestionById(answer.getQuestionId(),0);
        modelMap.addAttribute("question", question);
        modelMap.addAttribute("answer", answer);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("question/edit_answer");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/answer/edit_save")
    public DataVo editAnswer(@RequestParam(value = "id", required = false) String id,
                            @RequestParam(value = "content", required = false) String content,
                            ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("话题参数错误");
            }
            if (StringUtils.isBlank(content)) {
                return DataVo.failure("答案内容不能为空");
            }
            data=answerService.updateAnswerById(Long.parseLong(id),getUser().getUserId(),content);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //处理关注信息
    @ResponseBody
    @PostMapping(value = "/question/follow")
    public DataVo questionFollow(@RequestParam(value = "questionId", required = false) String questionId) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(questionId)) {
                return data=DataVo.failure("问题参数错误");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            data=questionService.QuestionFollow(Long.parseLong(questionId),getUser().getUserId());
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @ResponseBody
    @GetMapping(value = "/question/viewcount")
    public DataVo updateQuestionByViewCount(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("话题参数错误");
            }
            questionService.updateQuestionByViewCount(Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
