package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.article.model.Article;
import com.flycms.module.article.model.ArticleCategory;
import com.flycms.module.article.service.ArticleCategoryService;
import com.flycms.module.article.service.ArticleService;
import com.flycms.module.search.service.SolrService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
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
 * @Date: 10:57 2018/7/13
 */
@Controller
@RequestMapping("/system/article")
public class ArticleAdminController extends BaseController {
    @Autowired
    protected ArticleService articleService;
    @Autowired
    protected ArticleCategoryService articleCategoryService;
    @Autowired
    private SolrService solrService;

    //文章列表
    @GetMapping(value = "/article_list")
    public String getArticleList(@RequestParam(value = "title", required = false) String title,
                                 @RequestParam(value = "userId", required = false) String userId,
                                 @RequestParam(value = "createTime", required = false) String createTime,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "p", defaultValue = "1") int p,
                                 ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_article");
    }

    //查询单条文章数据信息
    @ResponseBody
    @GetMapping(value = "/findId")
    public DataVo getFindArticleId(@RequestParam(value = "id", required = false) String id, ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data = DataVo.failure("id参数错误");
            }
            Article article=articleService.findArticleById(Long.parseLong(id), 0);
            if(article==null) {
                return DataVo.failure("id错误或不存在！");
            }else {
                return DataVo.success("查询成功", article);
            }
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //文章审核操作
    @PostMapping("/article-status")
    @ResponseBody
    public DataVo updateArticleStatusById(@RequestParam(value = "id", required = false) String id,
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
        data = articleService.updateArticleStatusById(Long.parseLong(id),Integer.valueOf(status),Integer.valueOf(recommend));
        return data;
    }

    //添加文章
    @GetMapping(value = "/article_add")
    public String getAddArticle(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/add_article");
    }

    //保存添加文章
    @PostMapping("/article_save")
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
            data = articleService.addArticle(article);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //文章
    @PostMapping("/del")
    @ResponseBody
    public DataVo deleteArticleById(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        data = articleService.deleteArticleById(id);
        return data;
    }

    //文章列表
    @GetMapping(value = "/category_list")
    public String getCategoryList(@RequestParam(value = "title", required = false) String title,
                                 @RequestParam(value = "createTime", required = false) String createTime,
                                 @RequestParam(value = "p", defaultValue = "1") int pageNum,
                                 ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_article_category");
    }

    //按父级id查询id下所有地区列表
    @ResponseBody
    @RequestMapping(value = "/category_child")
    public List<ArticleCategory> getCategoryChild(@RequestParam(value = "parentId", defaultValue = "0") Long parentId){
        List<ArticleCategory> list=articleCategoryService.getCategoryListByFatherId(parentId);
        return list;
    }

    //按父级id查询id下所有地区列表
    @ResponseBody
    @RequestMapping(value = "/categorytree")
    public List<Map<String, Object>> getCategoryAllList(){
        List<ArticleCategory> categoryList = articleCategoryService.getCategoryAllList();
        List<Map<String, Object>> list =new ArrayList<Map<String, Object>>();
        Map<String, Object>map=null;
        for(ArticleCategory category:categoryList){
            map=new HashMap<>();
            map.put("id", category.getId());
            map.put("pId", category.getFatherId());
            map.put("name", category.getName());
            list.add(map);
        }
        return list;
    }

    //添加文章分类
    @GetMapping(value = "/category_add")
    public String getAddCategory(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/add_article_category");
    }

    //保存添加文章分类
    @PostMapping("/category_save")
    @ResponseBody
    public DataVo AddCategory(@RequestParam(value = "pid", required = false) String pid,@RequestParam(value = "name", required = false) String name){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(pid)) {
                return data = DataVo.failure("父级分类id不能为空");
            }
            if (!NumberUtils.isNumber(pid)) {
                data = DataVo.failure("父级分类id错误！");
            }
            if (StringUtils.isBlank(name)) {
                return DataVo.failure("分类名称不能为空");
            }
            data = articleCategoryService.addArticleCategory(Long.parseLong(pid),name);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑文章分类名称
    @PostMapping("/category_edit")
    @ResponseBody
    public DataVo editCategory(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "name", required = false) String name){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(id)) {
                return data = DataVo.failure("分类id不能为空");
            }
            if (!NumberUtils.isNumber(id)) {
                data = DataVo.failure("分类id错误！");
            }
            if (StringUtils.isBlank(name)) {
                return DataVo.failure("分类名称不能为空");
            }
            data = articleCategoryService.editArticleCategoryById(Long.parseLong(id),name);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑文章分类名称
    @PostMapping("/category_drags")
    @ResponseBody
    public DataVo editCategoryDrags(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "pId", required = false) String pId){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(id)) {
                return data = DataVo.failure("分类id不能为空");
            }
            if (!NumberUtils.isNumber(id)) {
                data = DataVo.failure("分类id错误！");
            }
            if (StringUtils.isBlank(pId)) {
                return DataVo.failure("未获取到父级id");
            }
            if (!NumberUtils.isNumber(pId)) {
                data = DataVo.failure("父级id错误！");
            }
            data = articleCategoryService.editCategoryDragsById(Long.parseLong(id),Long.parseLong(pId));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //删除文章分类
    @PostMapping("/category_delete")
    @ResponseBody
    public DataVo deleteArticleCategoryById(@RequestParam(value = "id", required = false) String id){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(id)) {
                return data = DataVo.failure("分类id不能为空");
            }
            if (!NumberUtils.isNumber(id)) {
                data = DataVo.failure("分类id错误！");
            }
            data = articleCategoryService.deleteArticleCategoryById(Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @ResponseBody
    @RequestMapping(value = "/index_all_article")
    public DataVo indexAllArticle() {
        DataVo data = DataVo.failure("操作失败");
        try {
            solrService.indexAllArticle();
            data=DataVo.success("全部索引成功！");
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
