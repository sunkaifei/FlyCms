package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.brand.model.BrandCategory;
import com.flycms.module.brand.service.BrandService;
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
 * @Date: 13:41 2018/7/17
 */
@Controller
@RequestMapping("/system/brand")
public class BrandAdminController extends BaseController {
    @Autowired
    protected BrandService brandService;

    @GetMapping(value = "/brand_list")
    public String getBrandList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("brand/brand_list");
    }

    @GetMapping(value = "/brand_add")
    public String getAddBrand(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("brand/brand_add");
    }



    @GetMapping(value = "/brand_edit/{id}")
    public String getEditBrand(@PathVariable int id,ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("brand/brand_edit");
    }

    @GetMapping(value = "/category_list")
    public String getCategoryList(ModelMap modelMap){
        List<BrandCategory> categoryList=brandService.getAllCategoryList();
        modelMap.addAttribute("admin", getAdminUser());
        modelMap.addAttribute("categoryList", categoryList);
        return theme.getAdminTemplate("brand/category_list");
    }

    @GetMapping(value = "/category_add")
    public String getAddCategory(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("brand/category_add");
    }

    //保存添加品牌分类
    @PostMapping("/brand_category_save")
    @ResponseBody
    public DataVo addBrandCategory(@Valid BrandCategory category, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = brandService.addBrandCategory(category);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @GetMapping(value = "/category_edit/{id}")
    public String getEditCategory(@PathVariable int id, ModelMap modelMap){
        BrandCategory  category= brandService.findCategoryById(id);
        modelMap.addAttribute("admin", getAdminUser());
        modelMap.addAttribute("category", category);
        return theme.getAdminTemplate("brand/category_edit");
    }
}
