package com.flycms.module.favorite.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.article.model.Article;
import com.flycms.module.article.service.ArticleService;
import com.flycms.module.favorite.dao.FavoriteDao;
import com.flycms.module.favorite.model.Favorite;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.service.QuestionService;
import com.flycms.module.share.model.Share;
import com.flycms.module.share.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
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
    private QuestionService questionService;
    @Autowired
    protected ArticleService articleService;
    @Autowired
    protected ShareService shareService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //用户添加信息收藏
    @Transactional
    public DataVo addFavorite(Long userId,Integer infoType,Long infoId){
        DataVo data = DataVo.failure("操作失败");
        if(infoType==0){
            Question question=questionService.findQuestionById(infoId,2);
            if (question == null) {
                return data=DataVo.failure("您收藏的信息不存在！");
            }
        }else if(infoType==1){
            Article article=articleService.findArticleById(infoId, 2);
            if (article == null) {
                return data=DataVo.failure("您收藏的信息不存在！");
            }
        }else if(infoType==2){
            Share share=shareService.findShareById(infoId,2);
            if (share == null) {
                return data=DataVo.failure("您收藏的信息不存在！");
            }
        }else{
            data = DataVo.failure("信息类型不存在！");
        }
        if(this.checkFavoriteByUser(userId,infoType,infoId)){
            data = DataVo.failure("已成功收藏！");
        }else{
            Favorite favorite=new Favorite();
            SnowFlake snowFlake = new SnowFlake(2, 3);
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
        if(order==null){
            order="desc";
        }
        pageVo.setList(favoriteDao.getFavoriteList(userId, infoType,createTime,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(favoriteDao.getFavoriteCount(userId, infoType,createTime));
        return pageVo;
    }
}
