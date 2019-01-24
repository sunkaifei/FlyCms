package com.flycms.module.share.dao;

import com.flycms.module.share.model.ShareCategory;
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
 * @Date: 10:08 2018/7/7
 */
@Repository
public interface ShareCategoryDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加资源分享分类
    public int addShareCategory(ShareCategory shareCategory);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按分类id删除该资源分享分类信息
    public int deleteShareCategoryById(Long id);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改资源分享分类
    public int editShareCategoryById(ShareCategory shareCategory);


    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //查询资源分享分类名称是否存在
    public int checkShareCategoryByName(@Param("name") String name, @Param("id") Long id);

    //按id查询资源分享分类信息
    public ShareCategory findCategoryById(@Param("id") Long id, @Param("status") Integer status);

    //根据资源分享分类id查询所属的所有子类
    public List<ShareCategory> getCategoryListByFatherId(@Param("fatherId") Long fatherId);

    //查询所有资源分享分类
    public List<ShareCategory> getCategoryAllList();
}
