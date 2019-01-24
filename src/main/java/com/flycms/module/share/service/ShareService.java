package com.flycms.module.share.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ShortUrlUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.search.service.SolrService;
import com.flycms.module.share.dao.ShareDao;
import com.flycms.module.share.model.Share;
import com.flycms.module.share.model.ShareCount;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.service.TopicService;
import com.flycms.module.user.service.FeedService;
import com.flycms.module.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 13:14 2018/9/3
 */
@Service
public class ShareService {
    @Resource
    private ShareDao shareDao;
    @Autowired
    protected FeedService feedService;
    @Autowired
    protected TopicService topicService;
    @Autowired
    protected UserService userService;
    @Autowired
    private SolrService solrService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    @Transactional
    public DataVo saveShare(Share share)throws ParseException {
        DataVo data = DataVo.failure("操作失败");
        if (StringUtils.isBlank(share.getTags())) {
            return DataVo.failure("话题不能为空，最少添加一个");
        }
        String[] tags = share.getTags().split(","); //转换为数组
        if (tags.length>5) {
            return DataVo.failure("话题数不能大于5个");
        }
        if(this.checkShareByTitle(share.getTitle(),share.getUserId(),null)){
            return data = DataVo.failure("该分享标题已存在");
        }
        SnowFlake snowFlake = new SnowFlake(2, 3);
        share.setId(snowFlake.nextId());
        String code=this.shortUrl();
        share.setShortUrl(code);
        share.setTitle(StringEscapeUtils.escapeHtml4(share.getTitle()));
        share.setContent(StringEscapeUtils.escapeHtml4(share.getContent()));
        share.setDownloads(StringEscapeUtils.escapeHtml4(share.getDownloads()));
        share.setCreateTime(new Date());
        shareDao.saveShare(share);
        //添加分享属性关联
        shareDao.addShareCount(share.getUserId());
        if(share.getId()!=null){
            //添加用户feed信息
            feedService.addUserFeed(share.getUserId(),2,share.getId());
            if (!StringUtils.isBlank(share.getTags())) {
                for (String string : tags) {
                    if (string != " " && string.length()>=2) {
                        Topic tag=topicService.findTopicByTopic(string);
                        if(!topicService.checkTopicByTopic(string)){
                            Topic addtag=topicService.addTopic(string,"",0,1,0,1);
                            topicService.addTopicAndInfo(share.getId(),addtag.getId(),2,share.getStatus());
                        }else{
                            topicService.updateTopicByCount(tag.getId());
                            topicService.addTopicAndInfo(share.getId(),tag.getId(),2,share.getStatus());
                        }
                    }
                }
            }
            solrService.indexShareId(share.getId());
            data = DataVo.success("分享内容添加成功");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    @CacheEvict(value = "share", allEntries = true)
    @Transactional
    public DataVo deleteShareById(Long id) {
        DataVo data = DataVo.failure("操作失败");
        Share share=shareDao.findShareById(id,0);
        if(share==null){
            data=DataVo.failure("该信息不存在！");
        }
        shareDao.deleteShareById(id);
        //删除统计
        shareDao.deleteShareCountById(id);
        //删除评论内容
        shareDao.deleteShareCommentById(id);
        //按文章id删除用户顶或者踩记录
        shareDao.deleteAllShareVotesById(id);
        //按id删除分享和分类关联记录
        shareDao.deleteShareCategoryMergeById(id);
        feedService.deleteUserFeed(share.getUserId(),2,share.getId());
        topicService.deleteTopicAndInfoUpCount(2,share.getId());
        solrService.indexDeleteInfo(2,share.getId());
        userService.updateShareCount(share.getUserId());
        data = DataVo.jump("删除成功！","/system/share/share_list");
        return data;
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////

    public DataVo updateShareById(Share share) throws ParseException{
        DataVo data = DataVo.failure("操作失败");
        if (StringUtils.isBlank(share.getTags())) {
            return DataVo.failure("话题不能为空，最少添加一个");
        }
        if(share.getId()==null || "".equals(share.getId())){
            return DataVo.failure("更新分享内容ID不能为空！");
        }
        String[] tags = share.getTags().split(","); //转换为数组
        if (tags.length>5) {
            return DataVo.failure("话题数不能大于5个");
        }
        if(this.checkShareByTitle(share.getTitle(),share.getUserId(),share.getId())){
            return data = DataVo.failure("该分享标题已存在");
        }
        share.setTitle(StringEscapeUtils.escapeHtml4(share.getTitle()));
        share.setDownloads(StringEscapeUtils.escapeHtml4(share.getDownloads()));
        share.setUpdateTime(new Date());
        int totalCount=shareDao.updateShareById(share);
        if(totalCount > 0){
            //索引本条信息
            solrService.indexArticleId(share.getId());
            if (!StringUtils.isBlank(share.getTags())) {
                topicService.deleteTopicAndInfoUpCount(2,share.getId());
                for (String string : tags) {
                    if (string != " " && string.length()>=2) {
                        Topic tag=topicService.findTopicByTopic(string);
                        if(!topicService.checkTopicByTopic(string)){
                            Topic addtag=topicService.addTopic(string,"",0,1,0,1);
                            topicService.addTopicAndInfo(share.getId(),addtag.getId(),2,share.getStatus());
                        }else{
                            topicService.updateTopicByCount(tag.getId());
                            topicService.addTopicAndInfo(share.getId(),tag.getId(),2,share.getStatus());
                        }
                    }
                }
            }
            userService.updateArticleCount(share.getUserId());
            data = DataVo.jump("分享更新成功！","/share/"+share.getId());
        }else{
            data=DataVo.failure("更新失败！");
        }
        return data;
    }

    /**
     * 按id更新分享浏览数量统计
     *
     * @param shareId
     *         分享id
     * @return
     */
    public int updateShareViewCount(Long shareId){
        return shareDao.updateShareViewCount(shareId);
    }

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
    @CacheEvict(value = "share", allEntries = true)
    @Transactional
    public DataVo updateShareStatusById(Long id, Integer status, Integer recommend) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Share share=this.findShareById(id,0);
        if(share==null){
            return data = DataVo.failure("该信息不存在或已删除");
        }
        shareDao.updateShareStatusById(id,status,recommend);
        if(status == 1){
            //添加用户feed信息,如果存在在修改状态为1
            if(feedService.checkUserFeed(share.getUserId(),2,share.getId())){
                feedService.addUserFeed(share.getUserId(),2,share.getId());
            }else{
                feedService.updateuUserFeedById(2,share.getId(),1);
            }
            userService.updateShareCount(share.getUserId());
            solrService.indexShareId(share.getId());
        }else{
            //删除用户feed信息
            feedService.updateuUserFeedById(2,share.getId(),0);
            //更新用户问答数量
            userService.updateShareCount(share.getUserId());
            //删除索引
            solrService.indexDeleteInfo(2,share.getId());
        }
        data=DataVo.success("审核操作成功！");
        return data;
    }
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    @Cacheable(value = "share")
    public Share findShareByShorturl(String shortUrl){
        return shareDao.findShareByShorturl(shortUrl);
    }

    //按id查询分享信息
    public Share findShareById(Long id,Integer status){
        return shareDao.findShareById(id,status);
    }

    //按id查询分享统计信息
    public ShareCount findShareCountById(Long shareId){
        return shareDao.findShareCountById(shareId);
    }

    /**
     * 查询文章短域名是否存在
     *
     * @param shortUrl
     * @return
     */
    public boolean checkShareByShorturl(String shortUrl) {
        int totalCount = shareDao.checkShareByShorturl(shortUrl);
        return totalCount > 0 ? true : false;
    }

    public String shortUrl(){
        String[] aResult = ShortUrlUtils.shortUrl (null);
        String code=null;
        for ( int i = 0; i < aResult. length ; i++) {
            code=aResult[i];
            //查询问答短域名是否被占用
            if(!this.checkShareByShorturl(code)){
                break;
            }
        }
        return code;
    }
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
    public boolean checkShareByTitle(String title,Long userId,Long id) {
        int totalCount = shareDao.checkShareByTitle(title,userId,id);
        return totalCount > 0 ? true : false;
    }

    //查询信息总数
    public int getShareCount(String title, Long userId, String createTime, Integer status){
        return shareDao.getShareCount(title,userId,createTime,status);
    }

    //分享索引列表
    public List<Share> getShareIndexList(int pageNum, int rows){
        return shareDao.getShareIndexList(pageNum,rows);
    }

    /**
     * 分享翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Share> getShareListPage(String title, Long userId, String createTime, Integer status, String orderby, String order, int pageNum, int rows) {
        PageVo<Share> pageVo = new PageVo<Share>(pageNum);
        pageVo.setRows(rows);
        List<Share> list = new ArrayList<Share>();
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(shareDao.getShareList(title,userId,createTime,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(shareDao.getShareCount(title,userId,createTime,status));
        return pageVo;
    }
}
