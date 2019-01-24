package com.flycms.module.share.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.article.model.ArticleCategory;
import com.flycms.module.share.dao.ShareCategoryDao;
import com.flycms.module.share.model.ShareCategory;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 18:36 2018/11/22
 */
@Service
public class ShareCategoryService {

    @Autowired
    protected ShareCategoryDao shareCategoryDao;

    private List<Integer> parentList = new ArrayList<Integer>();

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////

    //ztree添加分类
    @Transactional
    public DataVo addShareCategory(Long pid, String name){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkShareCategoryByName(name,null)){
            return data = DataVo.failure("分类名称不得重复");
        }
        ShareCategory category=new ShareCategory();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        category.setId(snowFlake.nextId());
        category.setFatherId(pid);
        category.setName(name);
        shareCategoryDao.addShareCategory(category);
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

    //后台添加分类
    @Transactional
    public DataVo addShareCategory(ShareCategory shareCategory){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkShareCategoryByName(shareCategory.getName(),null)){
            return data = DataVo.failure("分类名称不得重复");
        }
        //转换为数组
        String[] str = shareCategory.getCategoryId().split(",");
        SnowFlake snowFlake = new SnowFlake(2, 3);
        shareCategory.setId(snowFlake.nextId());
        shareCategory.setFatherId(Long.parseLong(str[str.length - 1]));
        shareCategoryDao.addShareCategory(shareCategory);
        if(shareCategory.getId()>0){
            data = DataVo.success("添加成功");
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    @Transactional
    public DataVo deleteShareCategoryById(Long id) {
        DataVo data = DataVo.failure("操作失败");
        int totalCount =shareCategoryDao.deleteShareCategoryById(id);
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
    public DataVo editShareCategoryById(Long id,String name){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkShareCategoryByName(name,id)){
            return data = DataVo.failure("分类名称已存在！");
        }
        ShareCategory category=new ShareCategory();
        category.setId(id);
        category.setName(name);
        shareCategoryDao.editShareCategoryById(category);
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
        ShareCategory category=new ShareCategory();
        category.setId(id);
        category.setFatherId(pId);
        shareCategoryDao.editShareCategoryById(category);
        if(category.getId()>0){
            data = DataVo.success("修改成功");
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }

    //后台修改分类
    @Transactional
    public DataVo editShareCategory(ShareCategory shareCategory){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkShareCategoryByName(shareCategory.getName(),shareCategory.getId())){
            return data = DataVo.failure("分类名称不得重复");
        }
        if (StringUtils.isBlank(shareCategory.getId().toString())) {
            return data = DataVo.failure("分类id不能为空");
        }
        if (!NumberUtils.isNumber(shareCategory.getId().toString())) {
            return data = DataVo.failure("分类id错误！");
        }
        //转换为数组
        String[] str = shareCategory.getCategoryId().split(",");
        if((Integer.valueOf(str[str.length - 1])).equals(shareCategory.getId())){
            return data = DataVo.failure("不能选自己为父级目录");
        }
        shareCategory.setFatherId(Long.parseLong(str[str.length - 1]));
        shareCategoryDao.editShareCategoryById(shareCategory);
        if(shareCategory.getId()>0){
            data = DataVo.success("更新成功");
        }else{
            data = DataVo.failure("添加失败！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询资源分享分类信息
    public ShareCategory findCategoryById(Long id,Integer status){
        return shareCategoryDao.findCategoryById(id,status);
    }

    /**
     * 查询资源分享分类名称是否已存在！
     *
     * @param id
     *         查询id
     * @param status
     *         审核状态
     * @return
     */
    public boolean checkCategoryById(Long id,Integer status) {
        return shareCategoryDao.findCategoryById(id,status)==null;
    }

    /**
     * 查询资源分享分类名称是否已存在！
     *
     * @param name
     *         分类名称
     * @return
     */
    public boolean checkShareCategoryByName(String name,Long id) {
        int totalCount = shareCategoryDao.checkShareCategoryByName(name,id);
        return totalCount > 0 ? true : false;
    }

    //根据资源分享分类id查询所属的所有子类
    public List<ShareCategory> getCategoryListByFatherId(Long fatherId){
        return shareCategoryDao.getCategoryListByFatherId(fatherId);
    }

    //查询所有资源分享分类
    public List<ShareCategory> getCategoryAllList(){
        return shareCategoryDao.getCategoryAllList();
    }

    private List<String> sonList = new ArrayList<String>();

    public String getFatherId(List<ShareCategory> itemList, ShareCategory node){
        sonList.clear();
        List<String> fatherList=this.getFatherNode(itemList,node);
        Collections.reverse(fatherList);
        return StringUtils.join(fatherList, ",");
    }

    /**
     * 递归获取所有的父类根节点
     *
     * @param itemList
     * @param node
     * @return
     */
    public List<String> getFatherNode(List<ShareCategory> itemList, ShareCategory node) {
        ShareCategory category = null;
        for (ShareCategory item : itemList) {
            if(item.getId().equals(node.getId())){
                sonList.add(item.getFatherId().toString());
            }
            if(node.getFatherId()>0 && item.getId().equals(node.getFatherId())){
                category = new ShareCategory();
                category.setId(item.getId());
                category.setFatherId(item.getFatherId());
            }
        }
        if(category!=null && node.getFatherId()>=0 ){
            sonList=getFatherNode(itemList,category);
        }
        return sonList;
    }
}
