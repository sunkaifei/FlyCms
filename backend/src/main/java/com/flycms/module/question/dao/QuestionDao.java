package com.flycms.module.question.dao;

import com.flycms.module.question.model.Question;
import com.flycms.module.question.model.QuestionCount;
import com.flycms.module.question.model.QuestionFollow;
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
 * @Date: 2018-8-10
 */
@Repository
public interface QuestionDao {

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //增加问题信息
    public int addQuestion(Question question);

    //增加问题相关统计信息
    public int addQuestionCount(@Param("questionId") Long questionId);

    //添加关注问题关联
    public int addQuestionFollow(@Param("questionId") Long questionId,@Param("userId") Long userId);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按问题id删除主表信息
    public int deleteQuestionById(@Param("questionId") Long questionId);

    //按问题id删除问题相关统计信息
    public int deleteQuestionCountById(@Param("questionId") Long questionId);

    /** 按问题id和用户id删除关注关联信息，可以单独删除单个信息id关联的关注信息
    *
    * 如：deleteQuestionFollow(questionId,null);
    *
    */
    public int deleteQuestionFollow(@Param("questionId") Long questionId,@Param("userId") Long userId);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////

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
    public int updateQuestionById(@Param("id") Long id,@Param("status") Integer status,@Param("recommend")  Integer recommend);

    /**
     * 按id更新问题回答数量统计
     *
     * @param questionId
     *         问题id
     * @return
     */
    public int updateQuestionByAnswerCount(@Param("questionId") Long questionId);

    /**
     * 按id更新问题关注数量统计
     *
     * @param questionId
     *         问题id
     * @return
     */
    public int updateQuestionByFollowCount(@Param("questionId") Long questionId);

    /**
     * 按id更新问题浏览数量统计
     *
     * @param questionId
     *         问题id
     * @return
     */
    public int updateQuestionByViewCount(@Param("questionId") Long questionId);
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按shortUrl查询文章信息
    public Question findQuestionByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 按id查询问题信息
     *
     * @param id
     *         问题id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    public Question findQuestionById(@Param("id") Long id, @Param("status") int status);

    /**
     * 按id查询问题统计信息
     *
     * @param questionId
     *         问题id
     * @return
     */
    public QuestionCount findQuestionCountById(@Param("questionId") Long questionId);

    /**
     * 查询问答短域名是否被占用
     *
     * @param shortUrl
     * @return
     */
    public int checkQuestionByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 查询用户组名是否存在,如果id、userId不为空或者null，排除当前id意外检查是否已存在！
     *
     * @param title
     *         标题
     * @param userId
     *         用户id
     * @return
     */
    public int checkQuestionByTitle(@Param("title") String title,@Param("userId") Long userId);

    /**
     * 查询是否已关注该问题
     *
     * @param questionId
     *         问题id
     * @param userId
     *         用户id
     * @return
     */
    public int checkQuestionFollow(@Param("questionId") Long questionId,@Param("userId") Long userId);

    //查询所有问题数量
    public int getQuestionCount(@Param("title") String title,
                             @Param("userId") Long userId,
                             @Param("createTime") String createTime,
                             @Param("status") Integer status);

    //问题列表
    public List<Question> getQuestionList(@Param("title") String title,
                                          @Param("userId") Long userId,
                                          @Param("createTime") String createTime,
                                          @Param("status") Integer status,
                                          @Param("orderby") String orderby,
                                          @Param("order") String order,
                                          @Param("offset") Integer offset,
                                          @Param("rows") Integer rows);

    //问题与用户关联列表
    public List<QuestionFollow> getQuestionFollowList(@Param("questionId") Long questionId, @Param("userId") Long userId);

    //问题索引总数
    public int getQuestionIndexCount();

    //问题索引列表
    public List<Question> getQuestionIndexList(@Param("offset") Integer offset, @Param("rows") Integer rows);
}
