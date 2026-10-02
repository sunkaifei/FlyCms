package com.flycms.module.images.dao;

import com.flycms.module.images.model.Images;
import com.flycms.module.images.model.ImagesInfoMerge;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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
public interface ImagesDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * @param images
     * @return
     */
    public int addImages(Images images);

    /** 批量 id → {id,imgUrl,imgName}（W 批次控件回显） */
    public List<Map<String, Object>> findByIds(@Param("ids") List<Long> ids);

    /** 回填多尺寸副本 JSON（Q1 媒体库多尺寸） */
    int updateSizesById(@Param("id") Long id, @Param("sizes") String sizes);

    /**
     * 添加图片和信息关联记录
     *
     * @param imagesInfoMerge
     * @return
     */
    public int addImagesInfoMerge(ImagesInfoMerge imagesInfoMerge);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    /**
     * 按图片id删除图片信息
     *
     * @param id
     * @return
     */
    public int deleteImagesById(@Param("id") Long id);


    /**
     * 按信息分类和内容id删除图片信息
     *
     * @param channelId
     * @param tid
     * @return
     */
    public int deleteImagesByTid(@Param("channelId") Integer channelId, @Param("tid") Long tid);

    /**
     * 按图片路径删除数据
     *
     * @param tid
     *         信息id
     * @param imgurl
     *         图片地址
     * @return
     */
    public int deleteImagesByTidAndImgurl(@Param("tid") Long tid, @Param("imgurl") String imgurl);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////

    public int updateImagesById(Images images);

    //更新图片被使用次数
    public int updateImagesCount(Long id);

    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    public Images getImagesById(@Param("id") Long id);

    /**
     * 按信息类别和信息ID查询所有相关图片信息
     *
     * @param tid
     * @return
     */
    public List<Images> getImagesListByTid( @Param("tid") Long tid);

    /**
     * 按信息类型id和信息id查询第一个文章图片
     *
     * @param imgUrl
     * @return
     */
    public Images findImagesByImgurl(@Param("imgUrl") String imgUrl);


    /**
     * 用信息id和图片地址查询该图片是否存在
     * @param tid
     *         信息id
     * @param imgUrl
     *         图片地址
     * @return
     */
    public int checkImagesByTidAndImgurl(@Param("tid") Long tid,@Param("imgUrl") String imgUrl);

    /**
     * 查询图片路径是否存在
     *
     * @param imgUrl
     *        图片地址
     * @return
     */
    public int checkImagesByImgurl(@Param("imgUrl") String imgUrl);


    // ///////////////////////////////

    /**
     * 附件库分页列表
     *
     * @param keyword    文件名/路径模糊匹配
     * @param onlyOrphan true=只看孤儿（引用计数为 0 或已标记删除）
     */
    public List<Images> getImagesLibraryList(@Param("keyword") String keyword,
                                             @Param("onlyOrphan") Boolean onlyOrphan,
                                             @Param("offset") int offset,
                                             @Param("rows") int rows);

    public int getImagesLibraryCount(@Param("keyword") String keyword,
                                     @Param("onlyOrphan") Boolean onlyOrphan);

    /** 按 id 批量物理删除附件记录（仅用于孤儿清理） */
    public int deleteImagesByIds(@Param("ids") java.util.List<Long> ids);

    /** 统计孤儿附件数量 */
    public int countOrphanImages();

    public List<Images> getImagesByIds(@Param("ids") java.util.List<Long> ids);
}
