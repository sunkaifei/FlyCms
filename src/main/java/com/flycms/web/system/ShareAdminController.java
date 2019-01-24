package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.search.service.SolrService;
import com.flycms.module.share.model.Share;
import com.flycms.module.share.model.ShareCategory;
import com.flycms.module.share.service.ShareCategoryService;
import com.flycms.module.share.service.ShareService;
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
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:05 2018/9/3
 */
@Controller
@RequestMapping("/system/share")
public class ShareAdminController extends BaseController {
    @Autowired
    private ShareService shareService;
    @Autowired
    protected ShareCategoryService shareCategoryService;
    @Autowired
    private SolrService solrService;

    @GetMapping(value = "/list_share")
    public String shareList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_share");
    }

    @ResponseBody
    @RequestMapping(value = "/index_all_share")
    public DataVo indexAllShare() {
        DataVo data = DataVo.failure("操作失败");
        try {
            solrService.indexAllShare();
            data=DataVo.success("全部索引成功！");
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //分享列表
    @GetMapping(value = "/category_list")
    public String getShareList(@RequestParam(value = "title", required = false) String title,
                                  @RequestParam(value = "createTime", required = false) String createTime,
                                  @RequestParam(value = "p", defaultValue = "1") int pageNum,
                                  ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/list_share_category");
    }

    //查询单条问题数据信息
    @ResponseBody
    @GetMapping(value = "/findId")
    public DataVo getFindShareId(@RequestParam(value = "id", required = false) String id, ModelMap modelMap) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data = DataVo.failure("id参数错误");
            }
            Share question=shareService.findShareById(Long.parseLong(id),0);
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
    @PostMapping("/share-status")
    @ResponseBody
    public DataVo updateShareStatusById(@RequestParam(value = "id", required = false) String id,
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
        data = shareService.updateShareStatusById(Long.parseLong(id),Integer.valueOf(status),Integer.valueOf(recommend));
        return data;
    }

    //删除分享
    @PostMapping("/del")
    @ResponseBody
    public DataVo deleteShareById(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        data = shareService.deleteShareById(id);
        return data;
    }

    //按父级id查询id下所有地区列表
    @ResponseBody
    @RequestMapping(value = "/category_child")
    public List<ShareCategory> getCategoryChild(@RequestParam(value = "parentId", defaultValue = "0") Long parentId){
        List<ShareCategory> list=shareCategoryService.getCategoryListByFatherId(parentId);
        return list;
    }


    //ZTree所有分类列表
    @ResponseBody
    @RequestMapping(value = "/categorytree")
    public List<Map<String, Object>> getCategoryZtree(){
        List<ShareCategory> categoryList = shareCategoryService.getCategoryAllList();
        List<Map<String, Object>> list =new ArrayList<Map<String, Object>>();
        Map<String, Object>map=null;
        for(ShareCategory category:categoryList){
            map=new HashMap<>();
            map.put("id", category.getId());
            map.put("pId", category.getFatherId());
            map.put("name", category.getName());
            list.add(map);
        }
        return list;
    }

    //按所有分类列表
    @ResponseBody
    @RequestMapping(value = "/allcategory")
    public List<ShareCategory> getCategoryAllList(){
        List<ShareCategory> categoryList = shareCategoryService.getCategoryAllList();
        return categoryList;
    }

    //添加分享分类
    @GetMapping(value = "/category_add")
    public String getAddShareCategory(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/add_share_category");
    }

    //保存添加分享分类
    @PostMapping("/category_save")
    @ResponseBody
    public DataVo AddShareCategory(@Valid ShareCategory shareCategory, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = shareCategoryService.addShareCategory(shareCategory);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //保存ztree添加分享分类
    @PostMapping("/ztree_category_save")
    @ResponseBody
    public DataVo AddShareCategory(@RequestParam(value = "pid", required = false) String pid,@RequestParam(value = "name", required = false) String name){
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
            data = shareCategoryService.addShareCategory(Long.parseLong(pid),name);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //添加分享分类
    @GetMapping(value = "/category_edit")
    public String editCategory(@RequestParam(value = "id", required = false) String id,ModelMap modelMap){
        if (StringUtils.isBlank(id)) {
            modelMap.addAttribute("message", "id不能为空");
            return theme.getAdminTemplate("common/message_tip");
        }
        if (!NumberUtils.isNumber(id)) {
            modelMap.addAttribute("message", "id参数错误");
            return theme.getAdminTemplate("common/message_tip");
        }
        ShareCategory category = shareCategoryService.findCategoryById(Long.parseLong(id),0);
        if (category == null) {
            modelMap.addAttribute("message", "分类不存在");
            return theme.getAdminTemplate("common/message_tip");
        }
        String categoryId=shareCategoryService.getFatherId(shareCategoryService.getCategoryAllList(),category);
        category.setCategoryId(categoryId);
        modelMap.addAttribute("category", category);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("content/edit_share_category");
    }

    //编辑文章分类名称
    @PostMapping("/ztree_category_edit")
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
            data = shareCategoryService.editShareCategoryById(Long.parseLong(id),name);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑文章分类名称
    @PostMapping("/category_edit_save")
    @ResponseBody
    public DataVo editCategory(@Valid ShareCategory shareCategory, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = shareCategoryService.editShareCategory(shareCategory);
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
            data = shareCategoryService.editCategoryDragsById(Long.parseLong(id),Long.parseLong(pId));
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
            data = shareCategoryService.deleteShareCategoryById(Long.parseLong(id));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

}
