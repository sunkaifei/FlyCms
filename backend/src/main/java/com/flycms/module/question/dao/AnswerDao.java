package com.flycms.module.question.dao;

import com.flycms.module.question.model.Answer;
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
 * @Date: 10:08 2018/8/23
 */
@Repository
public interface AnswerDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //增加答案信息
    public int addAnswer(Answer answer);

    //增加答案相关统计信息
    public int addAnswerCount(@Param("answerId") Long answerId);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    //答案id删除该id信息
    public int deleteAnswerById(@Param("id") Long id);

    //按答案id删除答案相关统计信息
    public int deleteAnswerCountById(@Param("answerId") Long answerId);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////

    //修改答案审核状态
    public int updateAnswerStatusById(@Param("id") Long id,@Param("status") Integer status);

    //按id更新答案内容
    public int updateAnswerById(Answer answer);

    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    public Answer findAnswerById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 按id和用户id查询评论内容
     *
     * @param id
     *         答案id
     * @param userId
     *         用户id
     * @return
     */
    public Answer findAnswerByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询该用户同样答案内容是否已存在！
     *
     * @param userId
     *         用户id
     * @param content
     *         答案内容
     * @return
     */
    public int checkAnswerByContent( @Param("userId") Long userId,@Param("content") String content);

    /**
     * 按参数查询记录数
     *
     * @param questionId
     *         问题id
     * @param userId
     *         用户id
     * @param createTime
     *         添加时间
     * @param status
     *         审核状态
     * @return
     */
    public int getAnswerCount(@Param("questionId") Long questionId,
                                @Param("userId") Long userId,
                                @Param("createTime") String createTime,
                                @Param("status") Integer status);

    /**
     * 按参数查询答案列表
     *
     * @param questionId
     *         问题id
     * @param userId
     *         用户id
     * @param createTime
     *         添加时间
     * @param status
     *         审核状态
     * @param orderby
     *         需要排序的字段
     * @param order
     *         排序：desc asc
     * @param offset
     *         当前页数
     * @param rows
     *         每页条数
     * @return
     */
    public List<Answer> getAnswerList(@Param("questionId") Long questionId,
                                      @Param("userId") Long userId,
                                      @Param("createTime") String createTime,
                                      @Param("status") Integer status,
                                      @Param("orderby") String orderby,
                                      @Param("order") String order,
                                      @Param("offset") Integer offset,
                                      @Param("rows") Integer rows);

    //按问题id或者用户id查询答案列表
    public List<Answer> gettAnswerByQuestionIdList(@Param("questionId") Long questionId, @Param("userId") Long userId);

    //按问题id查询最新的第一条评论内容
    public Answer findNewestAnswerById(@Param("questionId") Long questionId);
}
