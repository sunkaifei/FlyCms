package com.flycms.module.share.dao;

import com.flycms.module.share.model.Share;
import com.flycms.module.share.model.ShareCount;
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
 * @Date: 20:08 2018/8/31
 */
@Repository
public interface ShareDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 保存分享信息
     *
     * @param share
     * @return
     */
    public int saveShare(Share share);

    //添加分享属性关联
    public int addShareCount(@Param("shareId") Long shareId);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    public int deleteShareById(Long id);

    public int deleteShareCountById(Long id);

    public int deleteShareCategoryMergeById(Long shareId);

    public int deleteShareCommentById(Long shareId);

    public int deleteAllShareVotesById(Long shareId);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //按id更新分享
    public int updateShareById(Share share);

    //按id更新分享浏览数量统计
    public int updateShareViewCount(@Param("shareId") Long shareId);

    /**
     * 按id更新问题审核状态
     *
     * @param id
     *         问题id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @param recommend
     *         0不推荐,1内容页推荐,2栏目页推荐,3专题页推荐,4首页推荐,5全站推荐
     * @return
     */
    public int updateShareStatusById(@Param("id") Long id,@Param("status") Integer status,@Param("recommend")  Integer recommend);
    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    //按id查询分享信息
    public Share findShareByShorturl(@Param("shortUrl") String shortUrl);

    //按id查询分享信息
    public Share findShareById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 按id查询分享统计信息
     *
     * @param shareId
     *         分享id
     * @return
     */
    public ShareCount findShareCountById(@Param("shareId") Long shareId);

    /**
     * 查询文章短域名是否存在
     *
     * @param shortUrl
     * @return
     */
    public int checkShareByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 查询该用户同样标题内容是否已存在
     *
     * @param title
     *         标题
     * @param userId
     *         用户id
     * @param id
     *         排除当前内容id
     * @return
     */
    public int checkShareByTitle(@Param("title") String title,@Param("userId") Long userId,@Param("id") Long id);

    //分享总数
    public int getShareCount(@Param("title") String title,
                                @Param("userId") Long userId,
                                @Param("createTime") String createTime,
                                @Param("status") Integer status);

    //分享列表
    public List<Share> getShareList(@Param("title") String title,
                                          @Param("userId") Long userId,
                                          @Param("createTime") String createTime,
                                          @Param("status") Integer status,
                                          @Param("orderby") String orderby,
                                          @Param("order") String order,
                                          @Param("offset") Integer offset,
                                          @Param("rows") Integer rows);

    //分享索引列表
    public List<Share> getShareIndexList(@Param("offset") Integer offset,
                                    @Param("rows") Integer rows);
}
