package com.flycms.module.favorite.service;

import com.flycms.core.utils.OrderbyUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.favorite.dao.FavoriteDao;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.favorite.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:06 2018/9/6
 */
@Service
public class FavoriteService {
    @Resource
    private FavoriteDao favoriteDao;
    @Autowired
    protected ModelDataService modelDataService;
    @Autowired
    protected ModelService modelService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //用户添加信息收藏
    @Transactional
    public DataVo addFavorite(Long userId,Integer infoType,Long infoId){
        DataVo data = DataVo.failure("操作失败");
        if(infoType==1){
            // U3：收藏目标校验改走自定义模型通道（infoType 1 = articles 模型发布态内容）
            com.flycms.module.model.model.Model articleModel =
                    modelService.findModelByCode("articles");
            java.util.Map<String, Object> row = articleModel == null ? null
                    : modelDataService.findDataById(articleModel.getId(), infoId);
            if (row == null || !"1".equals(String.valueOf(row.get("status")))) {
                return data=DataVo.failure("您收藏的信息不存在！");
            }
        }else{
            data = DataVo.failure("信息类型不存在！");
        }
        if(this.checkFavoriteByUser(userId,infoType,infoId)){
            data = DataVo.failure("已成功收藏！");
        }else{
            Favorite favorite=new Favorite();
            SnowFlake snowFlake = SnowFlake.getInstance();
            favorite.setId(snowFlake.nextId());
            favorite.setUserId(userId);
            favorite.setInfoType(infoType);
            favorite.setInfoId(infoId);
            favorite.setCreateTime(new Date());
            favoriteDao.addFavorite(favorite);
            data = DataVo.success("已添加收藏！");
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
    // /////        查詢      ////////
    // ///////////////////////////////
    /**
     * 查询收藏信息是否存在
     *
     * @param userId
     *         用户id
     * @param infoType
     *         信息类型id
     * @param infoId
     *         收藏信息id
     * @return
     */
    public boolean checkFavoriteByUser(Long userId,Integer infoType,Long infoId) {
        int totalCount = favoriteDao.checkFavoriteByUser(userId,infoType,infoId);
        return totalCount > 0 ? true : false;
    }

    /**
     *  收藏翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Favorite> getFavoriteListPage(Long userId,Integer infoType, String createTime, String orderby, String order, int pageNum, int rows) {
        PageVo<Favorite> pageVo = new PageVo<Favorite>(pageNum);
        pageVo.setRows(rows);
        List<Favorite> list = new ArrayList<Favorite>();
        if(orderby==null){
            orderby="a.score";
            }
        orderby = OrderbyUtils.check(orderby, "a.score");
        if(order==null){
            order="desc";
            }
        order = OrderbyUtils.check(order, "desc");
        pageVo.setList(favoriteDao.getFavoriteList(userId, infoType,createTime,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(favoriteDao.getFavoriteCount(userId, infoType,createTime));
        return pageVo;
    }
}
