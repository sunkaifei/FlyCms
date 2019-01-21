package com.flycms.web.front;

import com.alibaba.fastjson.JSONArray;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.article.model.*;
import com.flycms.module.article.service.ArticleCategoryService;
import com.flycms.module.article.service.ArticleService;
import com.flycms.module.other.service.FilterKeywordService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 19:77 2018/9/17
 */
@Controller
public class ArticleController extends BaseController {
    @Autowired
    protected ArticleService articleService;
    @Autowired
    protected FilterKeywordService filterKeywordService;
    @Autowired
    protected ArticleCategoryService articleCategoryService;

    //文章列表
    @GetMapping(value = {"/ac/","/ac/index","/ac/{id}" })
    public String getArticleList(@PathVariable(value = "id", required = false) String id, @RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        ArticleCategory category=null;
        if(id != null){
            if (!NumberUtils.isNumber(id)) {
                return theme.getPcTemplate("404");
            }
            category =articleCategoryService.findCategoryById(Long.parseLong(id),2);
            if(category==null){
                return theme.getPcTemplate("404");
            }
        }

        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("id", id);
        modelMap.addAttribute("category", category);
        modelMap.addAttribute("p", p);
        return theme.getPcTemplate("article/index");
    }

    //文章详细页面
    @GetMapping(value = "/a/{shortUrl}")
    public String getArticleById(@PathVariable(value = "shortUrl", required = false) String shortUrl, @RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        if (StringUtils.isBlank(shortUrl)) {
            return theme.getPcTemplate("404");
        }
        //查询文章内容信息，由于缓存问题，统计要实时更新，所以统计和内容分开查询
        Article article=articleService.findArticleByShorturl(shortUrl);
        if(article==null){
            return theme.getPcTemplate("404");
        }
        //违禁词过滤
        article.setTitle(filterKeywordService.doFilter(article.getTitle()));
        article.setContent(filterKeywordService.doFilter(article.getContent()));
        //查询统计信息
        ArticleCount count=articleService.findArticleCountById(article.getId());
        article.setCountDigg(count.getCountDigg());
        article.setCountBurys(count.getCountBurys());
        article.setCountView(count.getCountView());
        article.setCountComment(count.getCountComment());
        if (getUser() != null) {
            modelMap.addAttribute("user", getUser());
        }
        modelMap.addAttribute("article", article);
        modelMap.addAttribute("p", p);
        return theme.getPcTemplate("article/detail");
    }

    //文章详细页面
    @ResponseBody
    @RequestMapping(value = "/findArticleById/{id}")
    public DataVo findArticleById(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        DataVo data = DataVo.failure("操作失败");
        if (!NumberUtils.isNumber(id)) {
            return data = DataVo.failure("id参数错误");
        }
        Article article=articleService.findArticleById(Long.parseLong(id), 2);
        if(article==null){
            return data = DataVo.failure("该内容不存在或者未审核！");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("content",article.getContent());
        return DataVo.success(map);
    }

    //添加文章
    @GetMapping(value = "/ucenter/article/add")
    public String getAddArticle(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("article/add_article");
    }

    //保存添加文章
    @PostMapping("/ucenter/article/article_save")
    @ResponseBody
    public DataVo addAdminSave(@Valid Article article, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            article.setUserId(getUser().getUserId());
            data = articleService.addArticle(article);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //保存用户文章评论内容
    @PostMapping("/ucenter/article/comment_save")
    @ResponseBody
    public DataVo addArticleComment(@Valid ArticleComment articleComment, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            articleComment.setUserId(getUser().getUserId());
            data = articleService.addArticleComment(articleComment);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //修改文章
    @GetMapping(value = "/ucenter/article/edit-{id}")
    public String getEditArticle(@PathVariable(value = "id", required = false) String id,ModelMap modelMap){
        if (!NumberUtils.isNumber(id)) {
            return theme.getPcTemplate("404");
        }
        Article article=articleService.findArticleById(Long.parseLong(id), 0);
        if(article==null){
            return theme.getPcTemplate("404");
        }
        if (!getUser().getUserId().equals(article.getUserId())) {
            modelMap.addAttribute("message", "只能修改属于自己的文章！");
            return theme.getPcTemplate("message_tip");
        }
        //article.setContent(StringEscapeUtils.escapeHtml4(article.getContent()));
        modelMap.addAttribute("article", article);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("article/edit_article");
    }

    //保存修改文章
    @PostMapping("/ucenter/article/article_update")
    @ResponseBody
    public DataVo editArticleById(@Valid Article article, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            Article info=articleService.findArticleById(article.getId(),0);
            if(info==null){
                return data=DataVo.failure("该内容不存在！");
            }
            if (!getUser().getUserId().equals(info.getUserId())) {
                return data=DataVo.failure("只能修改属于自己的文章！");
            }
            article.setUserId(getUser().getUserId());
            article.setShortUrl(info.getShortUrl());
            data = articleService.editArticleById(article);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //按父级id查询id下所有分类列表
    @ResponseBody
    @RequestMapping(value = "/ucenter/article/category_child")
    public List<ArticleCategory> getCategoryChildList(@RequestParam(value = "parentId", defaultValue = "0") Long parentId){
        List<ArticleCategory> list=articleCategoryService.getCategoryListByFatherId(parentId);
        return list;
    }

    //文章浏览量更新
    @ResponseBody
    @GetMapping(value = "/article/viewcount")
    public DataVo updateArticleByViewCount(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("文章参数错误");
            }
            articleService.updateArticleViewCount(Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //处理顶（用户推荐）信息
    @ResponseBody
    @PostMapping(value = "/article/digg")
    public DataVo articleDigg(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "type", defaultValue = "0") String type) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if(!StringUtils.isBlank(id)){
                if (!NumberUtils.isNumber(id)) {
                    return data=DataVo.failure("id参数错误");
                }
            }else{
                return data=DataVo.failure("ID不能为空！");
            }
            if(!StringUtils.isBlank(type)){
                if (!NumberUtils.isNumber(type)) {
                    return data=DataVo.failure("信息类型参数错误");
                }
            }else{
                return data=DataVo.failure("信息类型不能为空！");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            ArticleVotes articleVotes=new ArticleVotes();
            articleVotes.setInfoType(Integer.valueOf(type));
            articleVotes.setUserId(getUser().getUserId());
            articleVotes.setInfoId(Long.parseLong(id));
            articleVotes.setDigg(1);
            data=articleService.updateArticleVotesById(articleVotes);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
