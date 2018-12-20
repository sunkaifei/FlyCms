package com.flycms.module.user.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.user.dao.FeedDao;
import com.flycms.module.user.model.Feed;
import com.flycms.module.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:46 2018/9/14
 */
@Service
public class FeedService {
    @Autowired
    private FeedDao feedDao;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 用户注册基本信息
     *
     * @param userId
     *        用户id
     * @param infoType
     *        信息类型：0是问题，1是文章，2是分享
     * @param infoId
     *        用户注册密码
     * @return
     */
    @Transactional
    public DataVo addUserFeed(Long userId,Integer infoType,Long infoId) {
        DataVo data = DataVo.failure("操作失败");
        if(this.checkUserFeed(userId,infoType,infoId)) {
            return DataVo.failure("该feed已存在");
        }
        Feed feed=new Feed();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        feed.setId(snowFlake.nextId());
        feed.setUserId(userId);
        feed.setInfoType(infoType);
        feed.setInfoId(infoId);
        int count=feedDao.addUserFeed(feed);
        if(count<=5) {
            return data = DataVo.success("feed添加成功！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    public Long deleteUserFeed(Long userId,Integer infoType,Long infoId){
        return feedDao.deleteUserFeed(userId,infoType,infoId);
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改该用户feed信息的审核状态
    public Long updateuUserFeedById(Integer infoType,Long infoId,Integer status){
        return feedDao.updateuUserFeedById(infoType,infoId,status);
    }

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询用户是否存在
    public boolean checkUserFeed(Long userId,Integer infoType,Long infoId){
        int totalCount = feedDao.checkUserFeed(userId,infoType,infoId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 用户信息流翻页查询
     *
     * @param userId
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Feed> getUserListFeedPage(Long userId,Integer status, int pageNum, int rows) {
        PageVo<Feed> pageVo = new PageVo<Feed>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(feedDao.getUserFeedList(userId,status,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(feedDao.getUserFeedCount(userId,status));
        return pageVo;
    }
}
