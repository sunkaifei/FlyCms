package com.flycms.module.user.dao;

import com.flycms.module.user.model.Feed;
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
public interface FeedDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加用户feed信息
    public int addUserFeed(Feed feed);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按用户feed信息
    public Long deleteUserFeed(@Param("userId") long userId,@Param("infoType") Integer infoType,@Param("infoId") long infoId);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改该用户feed信息的审核状态
    public Long updateuUserFeedById(@Param("infoType") Integer infoType,@Param("infoId") long infoId,@Param("status") Integer status);


    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //查询该用户feed是否存在
    public int checkUserFeed(@Param("userId") long userId,@Param("infoType") Integer infoType,@Param("infoId") long infoId);

    //查询用户信息流总数
    public int getUserFeedCount(@Param("userId") long userId,@Param("status") Integer status);

    //查询用户信息流列表
    public List<Feed> getUserFeedList(@Param("userId") long userId,
                                      @Param("status") Integer status,
                                      @Param("offset") int offset,
                                      @Param("rows") int rows);
}
