package com.flycms.module.brand.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.brand.dao.BrandDao;
import com.flycms.module.brand.model.BrandCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:54 2018/7/13
 */
@Service
public class BrandService {
    @Autowired
    protected BrandDao dao;

    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    //添加品牌分类
    public DataVo addBrandCategory(BrandCategory category){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkCategoryByName(category.getName())){
            return data=DataVo.failure("分类已存在！");
        }
        int totalCount=dao.addBrandCategory(category);
        if(totalCount > 0){
            data = DataVo.success("分类添加成功！");
        }else{
            data=DataVo.failure("添加失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////

    //检查文章标题是否存在
    public boolean checkCategoryByName(String name) {
        int totalCount = dao.checkCategoryByName(name);
        return totalCount > 0 ? true : false;
    }

    //按id查询权限组信息
    public BrandCategory findCategoryById(int id){
        return dao.findCategoryById(id);
    }

    //所有品牌分类列表
    public List<BrandCategory> getAllCategoryList(){
        return dao.getAllCategoryList();
    };
}
