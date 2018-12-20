package com.flycms.module.article.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.article.dao.ArticleCategoryDao;
import com.flycms.module.article.model.ArticleCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 文章分类处理服务类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 15:35 2018/9/18
 */
@Service
public class ArticleCategoryService {

    @Autowired
    protected ArticleCategoryDao articleCategoryDao;

    private List<Integer> parentList = new ArrayList<Integer>();

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////

    @Transactional
    public DataVo addArticleCategory(Long pid,String name){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkArticleCategoryByName(name,null)){
            return data = DataVo.failure("分类名称不得重复");
        }
        ArticleCategory category=new ArticleCategory();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        category.setId(snowFlake.nextId());
        category.setFatherId(pid);
        category.setName(name);
        articleCategoryDao.addArticleCategory(category);
        if(category.getId()>0){
            Map<String, Object> map=new HashMap<>();
            map.put("id", category.getId());
            map.put("pId", category.getFatherId());
            map.put("name", category.getName());
            data = DataVo.success("添加成功",map);
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    @Transactional
    public DataVo deleteArticleCategoryById(Long id) {
        DataVo data = DataVo.failure("操作失败");
        int totalCount =articleCategoryDao.deleteArticleCategoryById(id);
        if(totalCount>0){
            data = DataVo.success("删除成功！");
        }else{
            data = DataVo.failure("删除失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    @Transactional
    public DataVo editArticleCategoryById(Long id,String name){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkArticleCategoryByName(name,id)){
            return data = DataVo.failure("分类名称已存在！");
        }
        ArticleCategory category=new ArticleCategory();
        category.setId(id);
        category.setName(name);
        articleCategoryDao.editArticleCategoryById(category);
        if(category.getId()>0){
            data = DataVo.success("修改成功");
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }

    //拖拽操作保存分类归属和排序
    @Transactional
    public DataVo editCategoryDragsById(Long id,Long pId){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkCategoryById(id,0)){
            return data = DataVo.failure("分类不存在！");
        }
        ArticleCategory category=new ArticleCategory();
        category.setId(id);
        category.setFatherId(pId);
        articleCategoryDao.editArticleCategoryById(category);
        if(category.getId()>0){
            data = DataVo.success("修改成功");
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询分类信息
    public ArticleCategory findCategoryById(Long id,Integer status){
        return articleCategoryDao.findCategoryById(id,status);
    }

    /**
     * 查询分类名称是否已存在！
     *
     * @param id
     *         查询id
     * @param status
     *         审核状态
     * @return
     */
    public boolean checkCategoryById(Long id,Integer status) {
        return articleCategoryDao.findCategoryById(id,status)==null;
    }

    /**
     * 查询分类名称是否已存在！
     *
     * @param name
     *         分类名称
     * @return
     */
    public boolean checkArticleCategoryByName(String name,Long id) {
        int totalCount = articleCategoryDao.checkArticleCategoryByName(name,id);
        return totalCount > 0 ? true : false;
    }

    //根据分类id查询所属的所有子类
    public List<ArticleCategory> getCategoryListByFatherId(Long fatherId){
        return articleCategoryDao.getCategoryListByFatherId(fatherId);
    }

    //查询所有分类
    public List<ArticleCategory> getCategoryAllList(){
        return articleCategoryDao.getCategoryAllList();
    }

    /**
     * 
     * @Title: queryItemListByTmpl
     * @Description:
     * @param itemList
     * @param id
     * @return List<Map<String,Object>> 返回类型
     * @throws
     */
    /**
     * 根据list以及根节点id获取树形展示数据
     *
     * @param itemList
     *
     * @param node
     *
     * @return
     */
    public static List<Map<String, Object>> queryItemListByTmpl(List<ArticleCategory> itemList, ArticleCategory node) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            List<Map<String, Object>> rusult = new ArrayList<Map<String, Object>>();
            ArticleCategory nodemap = null;
            for (ArticleCategory item : itemList) {
                if(node!=null){
                    if (item.getFatherId()==node.getFatherId()) {
                        map = new HashMap<String, Object>();
                        map.put("id", item.getId());
                        map.put("fatherId", item.getFatherId());
                        map.put("name", item.getName());
                        //map.put("expanded", "true");
                        rusult.add(map);
                    }
                    if(node.getFatherId()>=0 && item.getId() == node.getFatherId()){
                        nodemap = new ArticleCategory();
                        if (item.getId()==node.getFatherId()) {
                            nodemap.setId(item.getId());
                            nodemap.setName(item.getName());
                            nodemap.setFatherId(item.getFatherId());
                        }
                    }
                }else {
                    if (0==node.getFatherId()) {
                        map = new HashMap<String, Object>();
                        map.put("id", item.getId());
                        map.put("fatherId", item.getFatherId());
                        map.put("name", item.getName());
                        //map.put("expanded", "true");
                        rusult.add(map);
                    }
                }
            }
            if(node!=null){
                if(node.getFatherId()>=0){
                    rusult=getSonTree(rusult, itemList,nodemap);
                }
            }
            return rusult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 
     * @Title: getSonTree
     * @Description: 递归获取所有的根节点
     * @param rusult
     * @param itemList
     * @return Map<String,Object> 返回类型
     * @throws
     */
    /**
     * 递归获取所有的父类根节点
     *
     * @param rusult
     * @param itemList
     * @param node
     * @return
     */
    private static List<Map<String, Object>> getSonTree(List<Map<String, Object>> rusult,List<ArticleCategory> itemList, ArticleCategory node) {
        List<Map<String, Object>> sonList = new ArrayList<Map<String, Object>>();
        Map<String, Object> sonMap;
        ArticleCategory nodemapz=null;
        for (ArticleCategory item : itemList) {
            if (node.getFatherId()==item.getFatherId()) {
                sonMap = new HashMap<String, Object>();
                sonMap.put("id", item.getId());
                sonMap.put("fatherId", item.getFatherId());
                sonMap.put("name", item.getName());
                //sonMap.put("expanded", "true");
                if(node.getId()==item.getId()){
                    sonMap.put("children", rusult);
                }
                sonList.add(sonMap);
            }
            if(node.getFatherId()>0 && item.getId() == node.getFatherId()){
                if (item.getId()==node.getFatherId()) {
                    nodemapz = new ArticleCategory();
                    nodemapz.setId(item.getId());
                    nodemapz.setName(item.getName());
                    nodemapz.setFatherId(item.getFatherId());
                }
            }
        }
        if(nodemapz!=null && node.getFatherId()>=0 ){
            sonList=getSonTree(sonList, itemList,nodemapz);
        }
        return sonList;
    }
}
