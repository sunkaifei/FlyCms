package com.flycms.module.article.service;

import com.flycms.core.utils.OrderbyUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ShortUrlUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.article.dao.ArticleDao;
import com.flycms.module.article.model.Article;
import com.flycms.module.article.model.ArticleComment;
import com.flycms.module.article.model.ArticleCommentVo;
import com.flycms.module.article.model.ArticleCount;
import com.flycms.module.article.model.ArticleVotes;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.images.service.ImagesService;
import com.flycms.module.search.service.SearchService;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.service.TopicService;
import com.flycms.module.user.service.FeedService;
import com.flycms.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 文章内容处理服务类
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:23 2018/7/13
 */
@Slf4j
@Service
public class ArticleService {
    @Autowired
    protected ArticleDao articleDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    private SearchService searchService;
    @Autowired
    protected FeedService feedService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected TopicService topicService;
    @Autowired
    protected ImagesService imagesService;
    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////
    //添加文章
    public DataVo addArticle(Article article)  throws Exception {
        DataVo data = DataVo.failure("操作失败");
        if(StringUtils.isBlank(article.getTitle())){
            return data=DataVo.failure("标题不能为空！");
        }
        if(StringUtils.isBlank(article.getContent())){
            return data=DataVo.failure("内容不能为空！");
        }
        if(StringUtils.isBlank(article.getCategoryId()) || article.getCategoryId().length() < 2){
            return data=DataVo.failure("必须选择文章分类！");
        }
        //转换为数组
        String[] str = article.getCategoryId().split(",");
        if((new Long(0)).equals(Long.parseLong(str[str.length - 1]))){
            return data=DataVo.failure("必须选择文章分类！");
        }
        if(this.checkArticleByTitle(article.getTitle(),article.getUserId(),0L)){
            return data=DataVo.failure("标题已存在！");
        }
        if (StringUtils.isBlank(article.getTags())) {
            return DataVo.failure("话题不能为空，最少添加一个");
        }
        String[] tags = article.getTags().split(","); //转换为数组
        if (tags.length>5) {
            return DataVo.failure("话题数不能大于5个");
        }
        article=this.addArticle(article,tags);
        //索引本条信息
        if (article.getStatus() == 1) {
            searchService.indexArticleId(article.getId());
        }
        //更新
        //this.weight(article,null);
        data = DataVo.jump("文章添加成功！","/a/"+article.getShortUrl());
        return data;
    }

    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public Article addArticle(Article article,String[] tags)   throws Exception {
        //转换为数组
        String[] str = article.getCategoryId().split(",");
        SnowFlake snowFlake = SnowFlake.getInstance();
        article.setId(snowFlake.nextId());
        String code=this.shortUrl();
        article.setShortUrl(code);
        article.setTitle(StringEscapeUtils.escapeHtml4(article.getTitle()));
        article.setCreateTime(new Date());
        // 投稿审核开关（阶段 H）：fly_article_audit=1 先审后发（落 status=0），0 直接发布（落 status=1）
        article.setStatus(this.resolveArticleStatusByAuditSwitch());
        article.setContent(imagesService.replaceContent(2,article.getId(),article.getUserId(),article.getContent()));
        int totalCount=articleDao.addArticle(article);
        if(totalCount > 0) {
            articleDao.addArticleAndCategory(article.getId(), article.getCategoryId(), Long.parseLong(str[str.length - 1]));
            //添加用户feed信息
            feedService.addUserFeed(article.getUserId(), 1, article.getId());
            //添加文章统计关联数据
            articleDao.addArticleCount(article.getId());
            if (!StringUtils.isBlank(article.getTags())) {
                for (String string : tags) {
                    if (string != " " && string.length() >= 2) {
                        Topic tag = topicService.findTopicByTopic(string);
                        if (!topicService.checkTopicByTopic(string)) {
                            Topic addtag = topicService.addTopic(string, "", 0, 1, 0, 1);
                            topicService.addTopicAndInfo(article.getId(), addtag.getId(), 1,article.getStatus());
                        } else {
                            topicService.updateTopicByCount(tag.getId());
                            topicService.addTopicAndInfo(article.getId(), tag.getId(), 1,article.getStatus());
                        }
                    }
                }
            }
            userService.updateArticleCount(article.getUserId());
        }
        return article;
    }


    //添加文章评论内容
    @Transactional
    public DataVo addArticleComment(ArticleComment articleComment) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        if(this.checkArticleComment(articleComment.getArticleId(),articleComment.getUserId(),articleComment.getContent())){
            return data=DataVo.failure("请勿重复替提交相同评论！");
        }
        articleComment.setCreateTime(new Date());
        // 评论审核开关（阶段 B）：fly_comment_audit=1 先审后显示（落 status=0），0 免审直接显示（落 status=1）
        if (articleComment.getStatus() == null) {
            articleComment.setStatus(configService.getIntKey("fly_comment_audit", 1) == 1 ? 0 : 1);
        }
        int totalCount=articleDao.addArticleComment(articleComment);
        if(totalCount > 0){
            //更新文章被评论的数量
            articleDao.updateArticleCommentCount(articleComment.getArticleId());
            if (articleComment.getStatus() != null && articleComment.getStatus() == 0) {
                data = DataVo.success("评论已提交，等待审核", DataVo.NOOP);
            } else {
                data = DataVo.success("评论内容成功提交", DataVo.NOOP);
            }
        }else{
            data=DataVo.failure("提交评论发生未知错误！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public DataVo deleteArticleById(Long id) {
        DataVo data = DataVo.failure("操作失败");
        Article article=articleDao.findArticleById(id,0);
        if(article==null){
            data=DataVo.failure("该信息不存在！");
        }
        articleDao.deleteArticleById(id);
        //删除统计
        articleDao.deleteArticleCountById(id);
        //删除评论内容
        articleDao.deleteArticleCommentById(id);
        //按文章id删除用户顶或者踩记录
        articleDao.deleteAllArticleVotesById(id);
        //按id删除文章统计关联
        articleDao.deleteArticleAndCcategoryById(id);
        feedService.deleteUserFeed(article.getUserId(),1,article.getId());
        topicService.deleteTopicAndInfoUpCount(1,article.getId());
        searchService.indexDeleteInfo(1,article.getId());
        userService.updateArticleCount(article.getUserId());
        data = DataVo.jump("删除成功！","/admin/article/article_list");
        return data;
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改文章
    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public DataVo editArticleById(Article article) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        if(StringUtils.isBlank(article.getTitle())){
            return data=DataVo.failure("标题不能为空！");
        }
        if(StringUtils.isBlank(article.getContent())){
            return data=DataVo.failure("内容不能为空！");
        }
        if(StringUtils.isBlank(article.getCategoryId()) || article.getCategoryId().length() < 2){
            return data=DataVo.failure("必须选择文章分类！");
        }
        //转换为数组
        String[] str = article.getCategoryId().split(",");
        if((new Long(0)).equals(Long.parseLong(str[str.length - 1]))){
            return data=DataVo.failure("必须选择文章分类！");
        }
        if(this.checkArticleByTitle(article.getTitle(),article.getUserId(),article.getId())){
            return data=DataVo.failure("标题已存在！");
        }
        if (StringUtils.isBlank(article.getTags())) {
            return DataVo.failure("话题不能为空，最少添加一个");
        }
        String[] tags = article.getTags().split(","); //转换为数组
        if (tags.length>5) {
            return DataVo.failure("话题数不能大于5个");
        }
        article.setUpdateTime(new Date());
        // 编辑后同样按投稿审核开关落状态（阶段 H）
        article.setStatus(this.resolveArticleStatusByAuditSwitch());
        article.setContent(imagesService.replaceContent(2,article.getId(),article.getUserId(),article.getContent()));
        int totalCount=articleDao.editArticleById(article);
        if(totalCount > 0){
            //修改分类信息
            articleDao.editArticleAndCcategoryById(article.getCategoryId(),Integer.valueOf(str[str.length-1]),article.getId());

            //索引本条信息
            if(article.getStatus()==1){
                searchService.indexArticleId(article.getId());
            }else{
                searchService.indexDeleteInfo(1,article.getId());
            }
            if (!StringUtils.isBlank(article.getTags())) {
                topicService.deleteTopicAndInfoUpCount(1,article.getId());
                for (String string : tags) {
                    if (string != " " && string.length()>=2) {
                        Topic tag=topicService.findTopicByTopic(string);
                        if(!topicService.checkTopicByTopic(string)){
                            Topic addtag=topicService.addTopic(string,"",0,1,0,1);
                            topicService.addTopicAndInfo(article.getId(),addtag.getId(),1,article.getStatus());
                        }else{
                            topicService.updateTopicByCount(tag.getId());
                            topicService.addTopicAndInfo(article.getId(),tag.getId(),1,article.getStatus());
                        }
                    }
                }
            }
            userService.updateArticleCount(article.getUserId());
            data = DataVo.jump("文章更新成功！","/a/"+article.getShortUrl());
        }else{
            data=DataVo.failure("更新失败！");
        }
        return data;
    }

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
    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public DataVo updateArticleStatusById(Long id, Integer status, Integer recommend) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Article article=articleDao.findArticleById(id,0);
        if(article==null){
            return data = DataVo.failure("该信息不存在或已删除");
        }
        articleDao.updateArticleStatusById(id,status,recommend);
        if(status == 1){
            //添加用户feed信息,如果存在在修改状态为1
            if(feedService.checkUserFeed(article.getUserId(),1,article.getId())){
                feedService.addUserFeed(article.getUserId(),1,article.getId());
            }else{
                feedService.updateuUserFeedById(1,article.getId(),1);
            }
            userService.updateArticleCount(article.getUserId());
            searchService.indexArticleId(article.getId());
            //更新权重
            //this.weight(article,null);
        }else{
            //修改用户feed信息
            feedService.updateuUserFeedById(1,article.getId(),0);
            //更新用户问答数量
            userService.updateArticleCount(article.getUserId());
            //删除索引
            searchService.indexDeleteInfo(1,article.getId());
        }
        data=DataVo.success("审核操作成功！");
        return data;
    }

    /**
     * 更新文章被评论的权重分值
     *
     * @param weight
     *         权重分值
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleWeight(Double weight,long articleId){
        return articleDao.updateArticleWeight(weight,articleId);
    }

    /**
     * 按id更新文章浏览数量统计
     *
     * @param articleId
     *         文章id
     * @return
     */
    public int updateArticleViewCount(long articleId){
        return articleDao.updateArticleViewCount(articleId);
    }

    /**
     * 添加文章顶或踩操作并更新统计
     *
     * @param articleVotes
     *         顶和踩实体类
     * @return
     */
    public DataVo updateArticleVotesById(ArticleVotes articleVotes){
        DataVo data = DataVo.failure("该信息不存在或已删除");
        if(articleVotes.getInfoType()==0){
            Article article=articleDao.findArticleById(articleVotes.getInfoId(),2);
            if(article==null){
                return data = DataVo.failure("该文章不存在或已删除");
            }
        }else if(articleVotes.getInfoType()==1){
            ArticleComment comment=articleDao.findArticleCommentById(articleVotes.getInfoId(),2);
            if(comment==null){
                return data = DataVo.failure("该评论不存在或已删除");
            }
        }else{
            return data = DataVo.failure("信息类型错误");
        }
        if(articleVotes.getDigg() != 1 && articleVotes.getBurys() != 1){
            return data = DataVo.failure("请确认操作参数是否正确！");
        }
        if(articleVotes.getDigg()!=null && articleVotes.getBurys()!=null){
            if(articleVotes.getDigg() == 1 && articleVotes.getBurys() == 1){
                return data = DataVo.failure("顶或踩只能二选一！");
            }
        }
        if(this.checkArticleVotes(articleVotes.getInfoType(),articleVotes.getInfoId(),articleVotes.getUserId())){
            articleDao.deleteArticleVotesById(articleVotes.getInfoType(),articleVotes.getInfoId(),articleVotes.getUserId());
            data=DataVo.failure(2,"已取消推荐");
        }else{
            articleVotes.setCreateTime(new Date());
            int totalCount=articleDao.addArticleVotes(articleVotes);
            if(totalCount>0){
                data = DataVo.success("已推荐", DataVo.NOOP);
            }else{
                data=DataVo.failure("操作失败，请联系管理员！");
            }
        }
        if(articleVotes.getInfoType()==0){
            if(articleVotes.getDigg()==1){
                articleDao.updateArticleDiggCount(articleVotes.getInfoId());
            }else if(articleVotes.getBurys()==1){
                articleDao.updateArticleBurysCount(articleVotes.getInfoId());
            }
        }else if(articleVotes.getInfoType()==1){
            if(articleVotes.getDigg()==1){
                articleDao.updateArticleCommentDiggCount(articleVotes.getInfoId());
            }else if(articleVotes.getBurys()==1){
                articleDao.updateArticleCommentBurysCount(articleVotes.getInfoId());
            }
        }
        return data;
    }
    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    /**
     * 按shortUrl查询文章信息
     *
     * @param shortUrl
     *         短域名字符串
     * @return
     */
    @Cacheable(value = "article")
    public Article findArticleByShorturl(String shortUrl){
        return articleDao.findArticleByShorturl(shortUrl);
    }

    /**
     * 按id查询文章信息
     *
     * @param id
     *         文章id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    @Cacheable(value = "article")
    public Article findArticleById(Long id, Integer status){
        return articleDao.findArticleById(id,status);
    }

    /**
     * 按id查询文章信息
     *
     * @param articleId
     *         需要查询的文章id
     * @return
     */
    public ArticleCount findArticleCountById(Long articleId){
        return articleDao.findArticleCountById(articleId);
    }

    /**
     * 查询文章短域名是否被占用
     *
     * @param shortUrl
     * @return
     */
    public boolean checkArticleByShorturl(String shortUrl) {
        int totalCount = articleDao.checkArticleByShorturl(shortUrl);
        return totalCount > 0 ? true : false;
    }

    public String shortUrl(){
        String[] aResult = ShortUrlUtils.shortUrl (null);
        String code=null;
        for ( int i = 0; i < aResult. length ; i++) {
            code=aResult[i];
            //查询文章短域名是否被占用
            if(!this.checkArticleByShorturl(code)){
                break;
            }
        }
        return code;
    }
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
    public boolean checkArticleByTitle(String title,Long userId,Long id) {
        int totalCount = articleDao.checkArticleByTitle(title,userId,id);
        return totalCount > 0 ? true : false;
    }

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
    public boolean checkArticleComment(Long articleId,Long userId,String content) {
        int totalCount = articleDao.checkArticleComment(articleId,userId,content);
        return totalCount > 0 ? true : false;
    }

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
    public boolean checkArticleVotes(Integer infoType,Long infoId,Long userId) {
        int totalCount = articleDao.checkArticleVotes(infoType,infoId,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 文章翻页查询
     *
     * @param title
     *         标题
     * @param createTime
     *         添加时间
     * @param pageNum
     *         当前页码
     * @param rows
     *         每页数量
     * @return
     */
    public PageVo<Article> getArticleListPage(String title, Long userId,String createTime, Integer status,String orderby, String order, int pageNum, int rows) {
        PageVo<Article> pageVo = new PageVo<Article>(pageNum);
        pageVo.setRows(rows);
        List<Article> list = new ArrayList<Article>();
        if(orderby==null){
            orderby="id";
            }
        orderby = OrderbyUtils.check(orderby, "id");
        if(order==null){
            order="desc";
            }
        order = OrderbyUtils.check(order, "desc");
        pageVo.setList(articleDao.getArticleList(title,userId,createTime,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(articleDao.getArticleCount(title,userId,createTime,status));
        return pageVo;
    }

    //查询文章总数
    public int getArticleIndexCount(){
        return articleDao.getArticleIndexCount();
    }

    //文章索引列表
    public List<Article> getArticleIndexList(int pageNum, int rows){
        return articleDao.getArticleIndexList(pageNum,rows);
    }

    /**
     * 按id查询文章评论信息
     *
     * @param id
     *         评论id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    public ArticleComment findArticleCommentById(Long id, Integer status){
        return articleDao.findArticleCommentById(id,status);
    }

    /**
     * 文章翻页查询
     *
     * @param articleId
     *         文章id
     * @param createTime
     *         添加时间
     * @param pageNum
     *         当前页码
     * @param rows
     *         每页数量
     * @return
     */
    @Cacheable(value = "article")
    public PageVo<ArticleComment> getArticleCommentListPage(Long articleId, Long userId,String createTime, Integer status,String orderby, String order, int pageNum, int rows) {
        PageVo<ArticleComment> pageVo = new PageVo<ArticleComment>(pageNum);
        pageVo.setRows(rows);
        List<ArticleComment> list = new ArrayList<ArticleComment>();
        if(orderby==null){
            orderby="id";
            }
        orderby = OrderbyUtils.check(orderby, "id");
        if(order==null){
            order="desc";
            }
        order = OrderbyUtils.check(order, "desc");
        pageVo.setList(articleDao.getArticleCommentList(articleId,userId,createTime,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(articleDao.getArticleCommentCount(articleId,userId,createTime,status));
        return pageVo;
    }

    // ///////////////////////////////
    // /////  评论审核（阶段 B1） /////
    // ///////////////////////////////

    /**
     * 评论审核分页列表（join 文章标题，按真实 status 过滤）
     *
     * @param status null=全部(排除已删除) 0未审 1正常 2未通过 3删除
     */
    public PageVo<ArticleCommentVo> getCommentAuditPage(Long articleId, Long userId, String createTime,
                                                        String keyword, Integer status, int pageNum, int rows) {
        PageVo<ArticleCommentVo> pageVo = new PageVo<ArticleCommentVo>(pageNum);
        pageVo.setRows(rows);
        String orderby = OrderbyUtils.check("id", "id");
        String order = OrderbyUtils.check("desc", "desc");
        pageVo.setList(articleDao.getCommentAuditList(articleId, userId, createTime, keyword, status,
                orderby, order, pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(articleDao.getCommentAuditCount(articleId, userId, createTime, keyword, status));
        return pageVo;
    }

    /**
     * 单条评论审核：0未审 1正常(通过) 2未通过(驳回) 3删除
     */
    @Transactional
    public DataVo auditComment(Long id, Integer status) {
        if (id == null || status == null) {
            return DataVo.failure("参数不完整");
        }
        if (status < 0 || status > 3) {
            return DataVo.failure("审核状态不合法");
        }
        ArticleComment comment = articleDao.findArticleCommentById(id, null);
        if (comment == null) {
            return DataVo.failure("评论不存在");
        }
        int rows = articleDao.updateCommentStatus(id, status);
        // 评论数随审核结果变化，同步刷新文章评论计数
        articleDao.updateArticleCommentCount(comment.getArticleId());
        return rows > 0 ? DataVo.success("操作成功") : DataVo.failure("操作失败");
    }

    /**
     * 批量审核（批量通过/驳回/删除）
     */
    @Transactional
    public DataVo batchAuditComment(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return DataVo.failure("请选择要操作的评论");
        }
        if (status == null || status < 0 || status > 3) {
            return DataVo.failure("审核状态不合法");
        }
        int rows = articleDao.batchUpdateCommentStatus(ids, status);
        return rows > 0 ? DataVo.success("已处理 " + rows + " 条") : DataVo.failure("操作失败");
    }

    /**
     * 删除评论（逻辑删除 status=3）
     */
    @Transactional
    public DataVo deleteComment(Long id) {
        if (id == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = articleDao.deleteCommentById(id);
        return rows > 0 ? DataVo.success("删除成功") : DataVo.failure("评论不存在");
    }

    /**
     * 批量删除评论（逻辑删除 status=3）
     */
    @Transactional
    public DataVo batchDeleteComment(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return DataVo.failure("请选择要删除的评论");
        }
        int rows = articleDao.batchDeleteComment(ids);
        return rows > 0 ? DataVo.success("已删除 " + rows + " 条") : DataVo.failure("操作失败");
    }

    /**
     * 按问题id查询最新的第一条评论内容
     *
     * @param articleId
     *         文章id
     * @return
     */
    public ArticleComment findNewestArticleById(Long articleId){
        return articleDao.findNewestArticleById(articleId);
    }

    //文章索引列表
    public List<ArticleComment> getArticleCommentByArticleId(Long articleId){
        return articleDao.getArticleCommentByArticleId(articleId);
    }

    // ///////////////////////////////////
    // /////  投稿审核开关（阶段 H） /////
    // ///////////////////////////////////

    /**
     * 投稿审核开关：config 键 {@code fly_article_audit}（0=直接发布，1=先审后发）。
     *
     * <p>兼容说明：老版本用的是 {@code user_article_verify}，其语义相反（1=直接发布、0=先审后发），
     * 且 {@code Integer.parseInt(configService.getStringByKey(...))} 在配置缺失时会抛
     * NumberFormatException（getStringByKey 返回空串），导致投稿直接 500。
     * 这里统一收敛到 fly_article_audit，并对老键做反向映射，保证既有站点行为不变。</p>
     *
     * @return 1=先审后发；0=直接发布
     */
    public int getArticleAuditSwitch() {
        String value = configService.getStringByKey("fly_article_audit");
        if (StringUtils.isBlank(value)) {
            // 老键兜底：语义相反
            String legacy = configService.getStringByKey("user_article_verify");
            if (StringUtils.isBlank(legacy)) {
                return 0;
            }
            return "1".equals(legacy.trim()) ? 0 : 1;
        }
        return "1".equals(value.trim()) ? 1 : 0;
    }

    /**
     * 按审核开关计算投稿落库状态
     *
     * @return 1=正常（直接发布）；0=待审核
     */
    public int resolveArticleStatusByAuditSwitch() {
        return getArticleAuditSwitch() == 1 ? 0 : 1;
    }

    /**
     * 待审/已审文章分页列表（status 为真实状态）
     *
     * @param status 0未审核 1正常 2审核未通过 3删除；null=全部
     */
    public PageVo<Article> getArticleAuditPage(String title, Long userId, String createTime,
                                               Integer status, String orderby, String order,
                                               int pageNum, int rows) {
        PageVo<Article> pageVo = new PageVo<Article>(pageNum);
        pageVo.setRows(rows);
        if (StringUtils.isBlank(orderby)) {
            orderby = "id";
        }
        orderby = OrderbyUtils.check(orderby, "id");
        if (StringUtils.isBlank(order)) {
            order = "desc";
        }
        order = OrderbyUtils.check(order, "desc");
        pageVo.setList(articleDao.getArticleAuditList(title, userId, createTime, status,
                orderby, order, pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(articleDao.getArticleAuditCount(title, userId, createTime, status));
        return pageVo;
    }

    /**
     * 单条审核
     *
     * @param status 1=通过 2=驳回
     * @param reason 驳回原因（驳回时必填）
     */
    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public DataVo auditArticle(Long id, Integer status, String reason, Long adminId) {
        if (id == null) {
            return DataVo.failure("参数错误");
        }
        List<Long> ids = new ArrayList<Long>();
        ids.add(id);
        return batchAuditArticle(ids, status, reason, adminId);
    }

    /**
     * 批量审核：通过置 status=1 并重建索引/feed；驳回置 status=2 并发站内信通知作者。
     *
     * @param status 1=通过 2=驳回
     * @param reason 驳回原因，驳回时必填（规划要求）
     */
    @CacheEvict(value = "article", allEntries = true)
    @Transactional
    public DataVo batchAuditArticle(List<Long> ids, Integer status, String reason, Long adminId) {
        if (ids == null || ids.isEmpty()) {
            return DataVo.failure("请选择要审核的内容");
        }
        if (status == null || (status != 1 && status != 2)) {
            return DataVo.failure("审核状态错误");
        }
        if (status == 2 && StringUtils.isBlank(reason)) {
            return DataVo.failure("驳回必须填写原因");
        }
        int rows = articleDao.batchUpdateArticleStatus(ids, status);
        if (rows <= 0) {
            return DataVo.failure("操作失败");
        }
        for (Long id : ids) {
            Article article = articleDao.findArticleByPK(id);
            if (article == null) {
                continue;
            }
            try {
                if (status == 1) {
                    // 通过：feed、用户计数、索引
                    if (!feedService.checkUserFeed(article.getUserId(), 1, article.getId())) {
                        feedService.addUserFeed(article.getUserId(), 1, article.getId());
                    } else {
                        feedService.updateuUserFeedById(1, article.getId(), 1);
                    }
                    userService.updateArticleCount(article.getUserId());
                    searchService.indexArticleId(article.getId());
                } else {
                    // 驳回：撤 feed、撤索引，并站内信通知作者
                    feedService.updateuUserFeedById(1, article.getId(), 0);
                    userService.updateArticleCount(article.getUserId());
                    searchService.indexDeleteInfo(1, article.getId());
                    sendAuditMessage(article, reason, adminId);
                }
            } catch (Exception e) {
                // feed/索引/站内信属附属动作，失败不回滚已完成的审核状态
                log.error("审核后续处理失败, articleId={}, err={}", id, e.getMessage());
            }
        }
        return DataVo.success(status == 1 ? "已通过 " + rows + " 条" : "已驳回 " + rows + " 条");
    }

    /**
     * 审核结果通知作者（U2 退役 message 模块后暂为空操作；
     * 平台统一通知能力由后续 E 阶段承接，届时恢复实现）
     */
    private void sendAuditMessage(Article article, String reason, Long adminId) {
        log.info("文章审核未通过通知(待平台通知能力承接), articleId={}, title={}", article.getId(), article.getTitle());
    }

    /** 待审文章数量（后台角标用） */
    public int countPendingArticle() {
        return articleDao.getArticleAuditCount(null, null, null, 0);
    }
}
