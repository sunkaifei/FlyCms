package com.flycms.module.topic.dao;

import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.model.TopicCategory;
import com.flycms.module.topic.model.TopicEdit;
import com.flycms.module.topic.model.TopicInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:08 2018/8/18
 */
@Repository
public interface TopicDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加话题
    public int addTopic(Topic topic);

    /**
     * 添加话题与个信息关联
     *
     * @param infoId
     *        用户id
     * @param topicId
     *        话题id
     * @param infoType
     *        信息类型，0问题，1文章，2分享
     * @param status
     *         信息显示状态，默认为显示，0不显示，1显示
     * @return
     */
    public int addTopicAndInfo(@Param("id") Long id,@Param("infoId") Long infoId,@Param("topicId") Long topicId,@Param("infoType") Integer infoType,@Param("status") Integer status);

    /**
     * 添加用户关注的关注的标签id
     *
     * @param userId
     *        用户id
     * @param topicId
     *        话题id
     * @param createTime
     *        添加时间
     * @return
     */
    public int addTopicAndUser(@Param("userId") Long userId,@Param("topicId") Long topicId,@Param("createTime") Date createTime);

    //添加话题分类信息
    public int addTopicCategory(TopicCategory topicCategory);

    //用户增加编辑话题信息
    public int addUserEditTopic(TopicEdit topicEdit);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    /**
     * 按id删除话题
     *
     * @param id
     * @return
     */
    public int deleteTopicById(@Param("id") Integer id);

    /**
     * 按用户id和话题id删除关注的话题信息
     *
     * @param userId
     *        用户id
     * @param topicId
     *        话题id
     * @return
     */
    public int deleteTopicbyUserId(@Param("userId") Long userId,@Param("topicId") Long topicId);


    /**
     * 按用户id和话题id删除关注的话题信息
     *
     * @param infoId
     *        信息id
     * @param topicId
     *        话题id
     * @param infoType
     *        信息类型
     * @return
     */
    public int deleteTopicAndInfo(@Param("infoId") Long infoId,@Param("topicId") Long topicId,@Param("infoType") Integer infoType);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 按id更新标签信息
     *
     * @param topic
     * @return
     */
    public int updateTopicById(Topic topic);

    /**
     * 按id更新话题统计信息
     *
     * @param id
     * @return
     */
    public int updateTopicByCount(@Param("id") long id);

    /**
     * 按id更新审核状态
     *
     * @param status
     * @param id
     * @return
     */
    public int updateTagStatus(@Param("status") Integer status,@Param("id") long id);

    /**
     * 该话题被关注数量
     *
     * @param id
     * @return
     */
    public int updateTopicFollowByCount(@Param("id") long id);
    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    //按id查询话题信息
    public Topic findTopicByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 按id查询话题
     *
     * @param id
     *        话题id
     * @param status
     *        审核状态：0所有状态，1未审核，2审核
     * @return
     */
    public Topic findTopicById(@Param("id") Long id,@Param("status") Integer status);

    /**
     * 按话题查询该话题信息
     *
     * @param topic
     *         话题
     * @return
     */
    public Topic findTopicByTopic(@Param("topic") String topic);

    /**
     * 查询文章短域名是否存在
     *
     * @param shortUrl
     * @return
     */
    public int checkTopicByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 按话题查询是否存在
     *
     * @param topic
     *         话题
     * @return
     */
    public int checkTopicByTopic(@Param("topic") String topic);

    /**
     * 查询用户下是否该关注标签
     *
     * @param userId
     *         用户id
     * @param topicId
     *         话题id
     * @return
     */
    public int checkTopicByUserId(@Param("userId") Long userId,@Param("topicId") Long topicId);

    /**
     * 查询标签所有数量
     *
     * @return
     */
    public int getTopicCount(
            @Param("topic") String topic,
            @Param("type") String type,
            @Param("isgood") Integer isgood,
            @Param("status") Integer status
    );

    /**
     * 查标签列表
     *
     * @param offset
     * @param rows
     * @return
     */
    public List<Topic> getTopicList(
            @Param("topic") String topic,
            @Param("type") String type,
            @Param("isgood") Integer isgood,
            @Param("status") Integer status,
            @Param("orderBy") String orderBy,
            @Param("order") String order,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    /**
     * 查标签列表
     *
     * @param offset
     * @param rows
     * @return
     */
    public List<Topic> getTopicRandList(
            @Param("topic") String topic,
            @Param("type") String type,
            @Param("isgood") Integer isgood,
            @Param("status") Integer status,
            @Param("orderBy") String orderBy,
            @Param("order") String order,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    /**
     * 查询所有tag信息
     *
     * @return
     */
    public List<Topic> allWord();

    /**
     * 查询话题关联的信息数量
     *
     * @param infoType
     *         分类id，0问答，1文章，2分享
     * @param topicId
     *         话题id
     * @param status
     *         信息显示状态，默认为显示，0不显示，1显示
     *
     * @return
     */
    public int getTopicAndInfoCount(
            @Param("infoType") Integer infoType,
            @Param("topicId") Long topicId,
            @Param("status") Integer status);

    /**
     *
     * 查询话题关联的信息list
     *
     * @param infoType
     *         分类id，0问答，1文章，2分享
     * @param topicId
     *         话题id
     * @param status
     *         信息显示状态，默认为显示，0不显示，1显示
     * @param orderBy
     * @param order
     * @param offset
     * @param rows
     * @return
     */
    public List<TopicInfo> getTopicAndInfoList(
            @Param("infoType") Integer infoType,
            @Param("topicId") Long topicId,
            @Param("status") Integer status,
            @Param("orderBy") String orderBy,
            @Param("order") String order,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);


    /**
     * 按文章id查询所有文章关联的话题
     *
     * @param infoType
     *        分类id，0问答，1文章，2分享
     * @param infoId
     *        文章id
     * @return
     */
    public List<Topic> getInfoByTopicList(@Param("infoType") Integer infoType,@Param("infoId") long infoId);

    /**
     * 按用户id查询所有关注标签数量
     *
     * @param userId
     * @return
     */
    public int getUserTagsCount(@Param("user_id") long userId);

    /**
     * 按用户id查询所有关注标签列表
     *
     * @param userId
     * @return
     */
    public List<Topic> getUserTagsList(@Param("user_id") long userId,
                                       @Param("offset") Integer offset,
                                       @Param("rows") Integer rows);
}
