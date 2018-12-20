package com.flycms.module.article.dao;

import com.flycms.module.article.model.ArticleCategory;
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
public interface ArticleCategoryDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加文章分类
    public int addArticleCategory(ArticleCategory articleCategory);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按分类id删除该分类信息
    public int deleteArticleCategoryById(Long id);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //添加文章分类
    public int editArticleCategoryById(ArticleCategory articleCategory);


    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //查询分类名称是否存在
    public int checkArticleCategoryByName(@Param("name") String name,@Param("id") Long id);

    //按id查询分类信息
    public ArticleCategory findCategoryById(@Param("id") Long id,@Param("status") Integer status);

    //根据分类id查询所属的所有子类
    public List<ArticleCategory> getCategoryListByFatherId(@Param("fatherId") Long fatherId);

    //查询所有分类
    public List<ArticleCategory> getCategoryAllList();
}
