package com.flycms.module.article.dao;

import com.flycms.module.article.model.*;
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
 * @Date: 9:24 2018/7/13
 */
@Repository
public interface ArticleDao {

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加文章
    public int addArticle(Article article);

    //添加文章统计关联数据
    public int addArticleCount(@Param("articleId") Integer articleId);

    //按id查询文章信息
    public int addArticleAndCategory(@Param("articleId") Integer articleId,@Param("categoryId") String categoryId,@Param("typeId") Integer typeId);

    //添加文章评论内容
    public int addArticleComment(ArticleComment articleComment);

    //添加文章顶与踩记录
    public int addArticleVotes(ArticleVotes articleVotes);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除文章信息
    public int deleteArticleById(@Param("id") Integer id);

    //按id删除文章和分类关联
    public int deleteArticleAndCcategoryById(@Param("articleId") Integer articleId);

    /**
     * 按信息类型id、文章id、用户id删除用户顶或者踩记录
     * 只要执行此操作就会删除本条信息的踩或者顶
     *
     * @param infoType
     * @param infoId
     * @param userId
     * @return
     */
    public int deleteArticleVotesById(@Param("infoType") Integer infoType,
                                      @Param("infoId") Integer infoId,
                                      @Param("userId") Integer userId);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改文章
    public int editArticleById(Article article);

    /**
     * 按id更新文章审核状态
     *
     * @param id
     *         问题id
     * @param status
     *         0未审核 1正常状态 2审核未通过 3删除
     * @param recommend
     *         0不推荐,1内容页推荐,2栏目页推荐,3专题页推荐,4首页推荐,5全站推荐
     * @return
     */
    public int updateArticleStatusById(@Param("id") Integer id,@Param("status") Integer status,@Param("recommend") Integer recommend);

    //按id更新文章分类
    public int editArticleAndCcategoryById(@Param("categoryId") String categoryId,@Param("typeId") Integer typeId,@Param("articleId") Integer articleId);

    /**
     * 更新文章被评论的数量
     *
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleCommentCount(@Param("articleId") Integer articleId);

    /**
     * 更新文章被踩的数量
     *
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleDiggCount(@Param("articleId") Integer articleId);

    /**
     * 更新文章被踩的数量
     *
     * @param articleId
     *          文章id
     * @return
     */
    public int updateArticleBurysCount(@Param("articleId") Integer articleId);

    /**
     * 更新文章评论被踩的数量
     *
     * @param id
     *         评论id
     * @return
     */
    public int updateArticleCommentDiggCount(@Param("id") Integer id);

    /**
     * 更新文章评论被踩的数量
     *
     * @param id
     *          评论id
     * @return
     */
    public int updateArticleCommentBurysCount(@Param("id") Integer id);

    /**
     * 更新文章被评论的权重分值
     *
     * @param weight
     *         权重分值
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleWeight(@Param("weight") Double weight,@Param("articleId") Integer articleId);

    //按id更新文章浏览数量统计
    public int updateArticleViewCount(@Param("articleId") Integer articleId);
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询文章信息
    public Article findArticleById(@Param("id") Integer id,@Param("status") Integer status);

    //按id查询文章统计信息
    public ArticleCount findArticleCountById(@Param("articleId") Integer articleId);

    /**
     * 查询文章标题是否存在
     *
     * @param title
     *         发布文章标题
     * @param userId
     *         用户id，可设置为null
     * @param id
     *         当修改内容检查重复标题时，排除当前文章id，不排除可设置为null
     * @return
     */
    public int checkArticleByTitle(@Param("title") String title,@Param("userId") Integer userId,@Param("id") Integer id);

    /**
     * 查询文章相同的评论内容是否已添加
     *
     * @param articleId
     *         文章id
     * @param userId
     *         用户id
     * @param content
     *         评论内容
     * @return
     */
    public int checkArticleComment(@Param("articleId") Integer articleId,@Param("userId") Integer userId,@Param("content") String content);

    /**
     * 按信息类型id、文章id、用户id查询用户顶或者踩记录是否发布过同样内容
     * 注：一个类型的信息一个用户只能有一条记录，或顶或者踩
     *
     * @param infoType
     *         信息类型，0文章，1评论
     * @param infoId
     *         信息id
     * @param userId
     *         用户id
     * @return
     */
    public int checkArticleVotes(@Param("infoType") Integer infoType, @Param("infoId") Integer infoId,@Param("userId") Integer userId);

    //查询所有文章数量
    public int getArticleCount(@Param("title") String title,
                               @Param("userId") Integer userId,
                               @Param("createTime") String createTime,
                               @Param("status") Integer status);

    //文章列表
    public List<Article> getArticleList(@Param("title") String title,
                                        @Param("userId") Integer userId,
                                        @Param("createTime") String createTime,
                                        @Param("status") Integer status,
                                        @Param("orderby") String orderby,
                                        @Param("order") String order,
                                        @Param("offset") Integer offset,
                                        @Param("rows") Integer rows);

    //文章索引总数
    public int getArticleIndexCount();

    //文章索引列表
    public List<Article> getArticleIndexList(@Param("offset") Integer offset, @Param("rows") Integer rows);

    /**
     * 按id查询文章评论信息
     *
     * @param id
     *         评论id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    public ArticleComment findArticleCommentById(@Param("id") Integer id,@Param("status") Integer status);

    //查询所有文章数量
    public int getArticleCommentCount(@Param("articleId") Integer articleId,
                               @Param("userId") Integer userId,
                               @Param("createTime") String createTime,
                               @Param("status") Integer status);

    //文章列表
    public List<ArticleComment> getArticleCommentList(@Param("articleId") Integer articleId,
                                        @Param("userId") Integer userId,
                                        @Param("createTime") String createTime,
                                        @Param("status") Integer status,
                                        @Param("orderby") String orderby,
                                        @Param("order") String order,
                                        @Param("offset") Integer offset,
                                        @Param("rows") Integer rows);



    /**
     * 按id更新文章审核状态
     *
     * @param articleId
     *         文章id
     * @param userId
     *         用户id
     * @return
     */
    public int findArticleVotes(@Param("articleId") Integer articleId,@Param("userId") Integer userId);

    /**
     * 按问题id查询最新的第一条评论内容
     *
     * @param articleId
     *         文章id
     * @return
     */
    public ArticleComment findNewestArticleById(@Param("articleId") Integer articleId);

    //文章所有评论列表
    public List<ArticleComment> getArticleCommentByArticleId(@Param("articleId") Integer articleId);
}
