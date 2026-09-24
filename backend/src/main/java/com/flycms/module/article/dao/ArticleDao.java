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
    public int addArticleCount(@Param("articleId") Long articleId);

    //按id查询文章信息
    public int addArticleAndCategory(@Param("articleId") Long articleId,@Param("categoryId") String categoryId,@Param("typeId") Long typeId);

    //添加文章评论内容
    public int addArticleComment(ArticleComment articleComment);

    //添加文章顶与踩记录
    public int addArticleVotes(ArticleVotes articleVotes);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除文章信息
    public Long deleteArticleById(@Param("id") Long id);

    //按id删除文章统计关联
    public Long deleteArticleCountById(@Param("articleId") Long articleId);

    //按id删除文章评论内容
    public Long deleteArticleCommentById(@Param("articleId") Long articleId);

    //按id删除文章和分类关联
    public Long deleteArticleAndCcategoryById(@Param("articleId") Long articleId);

    //按文章id删除用户顶或者踩记录
    public Long deleteAllArticleVotesById(@Param("articleId") Long articleId);

    /**
     * 按信息类型id、文章id、用户id删除用户顶或者踩记录
     * 只要执行此操作就会删除本条信息的踩或者顶
     *
     * @param infoType
     * @param infoId
     * @param userId
     * @return
     */
    public Long deleteArticleVotesById(@Param("infoType") Integer infoType,
                                      @Param("infoId") Long infoId,
                                      @Param("userId") Long userId);

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
    public int updateArticleStatusById(@Param("id") Long id,@Param("status") Integer status,@Param("recommend") Integer recommend);

    //按id更新文章分类
    public int editArticleAndCcategoryById(@Param("categoryId") String categoryId,@Param("typeId") Integer typeId,@Param("articleId") Long articleId);

    /**
     * 更新文章被评论的数量
     *
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleCommentCount(@Param("articleId") Long articleId);

    /**
     * 更新文章被踩的数量
     *
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleDiggCount(@Param("articleId") Long articleId);

    /**
     * 更新文章被踩的数量
     *
     * @param articleId
     *          文章id
     * @return
     */
    public int updateArticleBurysCount(@Param("articleId") Long articleId);

    /**
     * 更新文章评论被踩的数量
     *
     * @param id
     *         评论id
     * @return
     */
    public int updateArticleCommentDiggCount(@Param("id") Long id);

    /**
     * 更新文章评论被踩的数量
     *
     * @param id
     *          评论id
     * @return
     */
    public int updateArticleCommentBurysCount(@Param("id") Long id);

    /**
     * 更新文章被评论的权重分值
     *
     * @param weight
     *         权重分值
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleWeight(@Param("weight") Double weight,@Param("articleId") Long articleId);

    //按id更新文章浏览数量统计
    public int updateArticleViewCount(@Param("articleId") Long articleId);
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按shortUrl查询文章信息
    public Article findArticleByShorturl(@Param("shortUrl") String shortUrl);

    //按id查询文章信息
    public Article findArticleById(@Param("id") Long id,@Param("status") Integer status);

    //按id查询文章统计信息
    public ArticleCount findArticleCountById(@Param("articleId") Long articleId);

    /**
     * 查询文章短域名是否存在
     *
     * @param shortUrl
     * @return
     */
    public int checkArticleByShorturl(@Param("shortUrl") String shortUrl);


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
    public int checkArticleByTitle(@Param("title") String title,@Param("userId") Long userId,@Param("id") Long id);

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
    public int checkArticleComment(@Param("articleId") Long articleId,@Param("userId") Long userId,@Param("content") String content);

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
    public int checkArticleVotes(@Param("infoType") Integer infoType, @Param("infoId") Long infoId,@Param("userId") Long userId);

    //查询所有文章数量
    public int getArticleCount(@Param("title") String title,
                               @Param("userId") Long userId,
                               @Param("createTime") String createTime,
                               @Param("status") Integer status);

    //文章列表
    public List<Article> getArticleList(@Param("title") String title,
                                        @Param("userId") Long userId,
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
    public ArticleComment findArticleCommentById(@Param("id") Long id,@Param("status") Integer status);

    //查询所有文章数量
    public int getArticleCommentCount(@Param("articleId") Long articleId,
                               @Param("userId") Long userId,
                               @Param("createTime") String createTime,
                               @Param("status") Integer status);

    //文章列表
    public List<ArticleComment> getArticleCommentList(@Param("articleId") Long articleId,
                                        @Param("userId") Long userId,
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
    public int findArticleVotes(@Param("articleId") Long articleId,@Param("userId") Long userId);

    /**
     * 按问题id查询最新的第一条评论内容
     *
     * @param articleId
     *         文章id
     * @return
     */
    public ArticleComment findNewestArticleById(@Param("articleId") Long articleId);

    //文章所有评论列表
    public List<ArticleComment> getArticleCommentByArticleId(@Param("articleId") Long articleId);

    // ///////////////////////////////
    // /////  评论审核（阶段 B1） /////
    // ///////////////////////////////

    /**
     * 评论审核列表（join 文章标题）。
     * 与既有 getArticleCommentList 的区别：status 按「真实状态」过滤（语义直给），
     * 旧方法用的是 1~4 的筛选器编码，两者不混用。
     *
     * @param status null=全部(排除已删除) 0未审 1正常 2未通过 3删除
     */
    public List<ArticleCommentVo> getCommentAuditList(@Param("articleId") Long articleId,
                                                      @Param("userId") Long userId,
                                                      @Param("createTime") String createTime,
                                                      @Param("keyword") String keyword,
                                                      @Param("status") Integer status,
                                                      @Param("orderby") String orderby,
                                                      @Param("order") String order,
                                                      @Param("offset") int offset,
                                                      @Param("rows") int rows);

    public int getCommentAuditCount(@Param("articleId") Long articleId,
                                    @Param("userId") Long userId,
                                    @Param("createTime") String createTime,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    /** 按评论主键更新审核状态 */
    public int updateCommentStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 批量更新审核状态。
     * 注意：既有 deleteArticleCommentById 实际是按 article_id 删（会误删整篇评论），
     * 这里按主键精确操作。
     */
    public int batchUpdateCommentStatus(@Param("ids") java.util.List<Long> ids, @Param("status") Integer status);

    /** 按评论主键删除（逻辑删除：置 status=3） */
    public int deleteCommentById(@Param("id") Long id);

    /** 按评论主键批量逻辑删除 */
    public int batchDeleteComment(@Param("ids") java.util.List<Long> ids);

    /** 按评论主键物理删除 */
    public int removeCommentById(@Param("id") Long id);

    // ///////////////////////////////
    // /////  投稿审核（阶段 H） /////
    // ///////////////////////////////

    /**
     * 待审/已审文章列表。
     * 与既有 getArticleList 的区别：status 按「真实状态」精确过滤，
     * 旧方法用的是 0~4 的筛选器编码（status=1 对应 a.status=0），两者不混用。
     *
     * @param status 真实状态：0未审核 1正常 2审核未通过 3删除；null=全部
     */
    public List<Article> getArticleAuditList(@Param("title") String title,
                                             @Param("userId") Long userId,
                                             @Param("createTime") String createTime,
                                             @Param("status") Integer status,
                                             @Param("orderby") String orderby,
                                             @Param("order") String order,
                                             @Param("offset") Integer offset,
                                             @Param("rows") Integer rows);

    public int getArticleAuditCount(@Param("title") String title,
                                    @Param("userId") Long userId,
                                    @Param("createTime") String createTime,
                                    @Param("status") Integer status);

    /**
     * 批量更新文章审核状态（只改 status，不动 recommend）
     */
    public int batchUpdateArticleStatus(@Param("ids") java.util.List<Long> ids, @Param("status") Integer status);

    /** 按主键查询文章（不区分状态，供审核取作者/标题用） */
    public Article findArticleByPK(@Param("id") Long id);
}
