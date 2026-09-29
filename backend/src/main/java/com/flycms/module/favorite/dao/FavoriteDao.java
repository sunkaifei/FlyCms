package com.flycms.module.favorite.dao;

import com.flycms.module.favorite.model.Favorite;
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
public interface FavoriteDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加收藏信息
    public int addFavorite(Favorite favorite);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除收藏信息
    public int deleteFavoriteById(@Param("userId") Integer userId,@Param("infoType") Integer infoType,@Param("infoId") Integer infoId);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    public Favorite findFavoriteById(@Param("id") Integer id);

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
    public int checkFavoriteByUser(@Param("userId") Long userId,@Param("infoType") Integer infoType,@Param("infoId") Long infoId);

    //查询收藏总数
    public int getFavoriteCount(@Param("userId") Long userId,
                            @Param("infoType") Integer infoType,
                            @Param("createTime") String createTime);

    //收藏列表
    // /////////////////// E7 收藏平台化 ///////////////////

    /** 平台化收藏列表（按用户，可选限定模型；时间倒序） */
    public List<Favorite> listByUser(@Param("userId") Long userId,
                                     @Param("modelCode") String modelCode,
                                     @Param("offset") int offset,
                                     @Param("rows") int rows);

    public int countByUser(@Param("userId") Long userId,
                           @Param("modelCode") String modelCode);

    public int deleteByUserAndTarget(@Param("userId") Long userId,
                                     @Param("modelCode") String modelCode,
                                     @Param("infoId") Long infoId);

    /** 收藏是否已存在（平台化：用户+模型+目标） */
    public int checkByUserAndTarget(@Param("userId") Long userId,
                                    @Param("modelCode") String modelCode,
                                    @Param("infoId") Long infoId);

    public List<Favorite> getFavoriteList(@Param("userId") Long userId,
                                      @Param("infoType") Integer infoType,
                                      @Param("createTime") String createTime,
                                      @Param("orderby") String orderby,
                                      @Param("order") String order,
                                      @Param("offset") Integer offset,
                                      @Param("rows") Integer rows);
}
