package com.flycms.module.question.dao;

import com.flycms.module.order.model.Order;
import com.flycms.module.question.model.Images;
import com.flycms.module.question.model.ImagesInfoMerge;
import com.flycms.module.question.model.Question;
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
public interface ImagesDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * @param images
     * @return
     */
    public int addImages(Images images);

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


    /**
     * 按信息id查询所有关联图片的数量
     *
     * @param channelId
     *        信息id
     * @param img_width
     *        图片宽度
     * @param img_height
     *        图片高度
     * @return
     */
    public int getImagesByTidListCount(
            @Param("channelId") Integer channelId,
            @Param("img_width") Integer img_width,
            @Param("img_height") Integer img_height);

    /**
     *按信息类别id查询所有关联的图片
     *
     * @param channelId
     *        信息类别id
     * @param imgWidth
     *        图片宽度
     * @param imgHeight
     *        图片高度
     * @param offset
     *        翻页起始数
     * @param rows
     *        查询记录条数
     * @return
     */
    public List<Question> getImagesByTidList(
            @Param("channelId") Integer channelId,
            @Param("imgWidth") Integer imgWidth,
            @Param("imgHeight") Integer imgHeight,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);
}
