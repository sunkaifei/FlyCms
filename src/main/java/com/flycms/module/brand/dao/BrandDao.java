package com.flycms.module.brand.dao;

import com.flycms.module.brand.model.BrandCategory;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:51 2018/7/13
 */
@Repository
public interface BrandDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加文章
    public int addBrandCategory(BrandCategory brandCategory);


    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //查询品牌分类是否存在
    public int checkCategoryByName(@Param("name") String name);

    //按ID查询品牌分类信息
    public BrandCategory findCategoryById(@Param("id") int id);

    //所有品牌分类列表
    public List<BrandCategory> getAllCategoryList();
}
