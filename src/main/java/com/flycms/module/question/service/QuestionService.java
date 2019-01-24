package com.flycms.module.question.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ShortUrlUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.question.dao.QuestionDao;
import com.flycms.module.question.model.Answer;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.model.QuestionCount;
import com.flycms.module.question.model.QuestionFollow;
import com.flycms.module.search.service.SolrService;
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

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class QuestionService {
    @Autowired
    protected QuestionDao questionDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    protected ImagesService imagesService;
    @Autowired
    protected AnswerService answerService;
    @Autowired
    protected TopicService topicService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected FeedService feedService;
    @Autowired
    private SolrService solrService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    @Transactional
    public DataVo addQuestion(String title,String content,String tags,Long userId) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        String[] ss = tags.split(","); //转换为数组
        if (ss.length>5) {
            return DataVo.failure("话题数不能大于5个");
        }

        Question question=new Question();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        question.setId(snowFlake.nextId());
        String code=this.shortUrl();
        question.setShortUrl(code);
        question.setUserId(userId);
        question.setTitle(StringEscapeUtils.escapeHtml4(title));
        question.setCreateTime(new Date());
        //内容保存本地并转换图片地址为本地
        if(content!=null){
            content=imagesService.replaceContent(1,question.getId(),question.getUserId(),content);
        }
        question.setContent(content);
        question.setStatus(Integer.parseInt(configService.getStringByKey("user_question_verify")));
        int totalCount=questionDao.addQuestion(question);
        if(totalCount>0){
            //增加问题相关统计信息
            questionDao.addQuestionCount(question.getId());
            if (!StringUtils.isBlank(tags)) {
                for (String string : ss) {
                    if (string != " " && string.length()>=2) {
                        Topic tag=topicService.findTopicByTopic(string);
                        if(!topicService.checkTopicByTopic(string)){
                            Topic addtag=topicService.addTopic(string,"",0,1,0,1);
                            topicService.addTopicAndInfo(question.getId(),addtag.getId(),0, question.getStatus());
                        }else{
                            topicService.updateTopicByCount(tag.getId());
                            topicService.addTopicAndInfo(question.getId(),tag.getId(),0, question.getStatus());
                        }
                    }
                }
            }
            if(question.getStatus()==1){
                //添加用户feed信息
                feedService.addUserFeed(userId,0,question.getId());
                userService.updateQuestionCount(userId);
                solrService.indexQuestionId(question.getId());
                data=DataVo.jump("已成功提交", "/q/" + question.getShortUrl());
            }else{
                data=DataVo.jump("已成功提交,等待审核", "/search?s=" + question.getTitle());
            }
        }else{
            return data= DataVo.failure("添加失败，未知原因，请联系管理员！");
        }
        return data;
    }

    //按问题id关注或取消
    @Transactional
    @CacheEvict(value="question", allEntries=true)
    public DataVo QuestionFollow(Long questionId,Long userId) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");

        if(!this.checkQuestion(questionId,2)){
            return data;
        }
        if(this.checkQuestionFollow(questionId,userId)){
            questionDao.deleteQuestionFollow(questionId,userId);
            questionDao.updateQuestionByFollowCount(questionId);
            data=DataVo.failure(2,"已取消");
        }else{
            questionDao.addQuestionFollow(questionId,userId);
            questionDao.updateQuestionByFollowCount(questionId);
            data=DataVo.success("已关注");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除该问题所信息
    @Transactional
    public DataVo deleteQuestionById(Long id){
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Question question=questionDao.findQuestionById(id,0);
        if(question!=null){
            //删除当前id的问题
            questionDao.deleteQuestionById(id);
            List<Answer> answerlist=answerService.gettAnswerByQuestionIdList(question.getId(),null);
            for (Answer a_list:answerlist){
                answerService.deleteQuestionAndAnswerById(a_list.getId());
            }
            List<QuestionFollow> list=questionDao.getQuestionFollowList(question.getId(),null);
            for (QuestionFollow qf_list:list){
                //更新所有关注的该问题统计数量
                userService.updateQuestionFollowCount(qf_list.getUserId());
            }
            //按问题id删除问题相关统计信息
            questionDao.deleteQuestionCountById(question.getId());

            feedService.deleteUserFeed(question.getUserId(),0,question.getId());

            topicService.deleteTopicAndInfoUpCount(0,question.getId());
            //清除本id相关联的关联数据
            questionDao.deleteQuestionFollow(question.getId(),null);
            //更新该问题用户的提问数量
            userService.updateQuestionCount(question.getUserId());
            //删除索引
            solrService.indexDeleteInfo(0,question.getId());
            data=DataVo.success("已成功删除");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 按id更新问题浏览数量统计
     *
     * @param id
     *         问题id
     * @return
     */
    public int updateQuestionByViewCount(Long id){
        return questionDao.updateQuestionByViewCount(id);
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
    @CacheEvict(value = "question", allEntries = true)
    @Transactional
    public DataVo updateQuestionStatusById(Long id, Integer status, Integer recommend) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Question question=this.findQuestionById(id,0);
        if(question==null){
            return data = DataVo.failure("该信息不存在或已删除");
        }
        questionDao.updateQuestionById(id,status,recommend);
        if(status == 1){
            //添加用户feed信息,如果存在在修改状态为1
            if(feedService.checkUserFeed(question.getUserId(),0,question.getId())){
                feedService.addUserFeed(question.getUserId(),0,question.getId());
            }else{
                feedService.updateuUserFeedById(0,question.getId(),1);
            }
            userService.updateQuestionCount(question.getUserId());
            solrService.indexQuestionId(question.getId());
        }else{
            //删除用户feed信息
            feedService.updateuUserFeedById(0,question.getId(),0);
            //更新用户问答数量
            userService.updateQuestionCount(question.getUserId());
            //删除索引
            solrService.indexDeleteInfo(0,question.getId());
        }
        data=DataVo.success("审核操作成功！");
        return data;
    }

    // ///////////////////////////////
    // /////        查询      ////////
    // ///////////////////////////////
    @Cacheable(value = "question",key="#shortUrl")
    public Question findQuestionByShorturl(String shortUrl){
        return questionDao.findQuestionByShorturl(shortUrl);
    }

    /**
     * 按id查询问题信息
     *
     * @param id
     *         问题id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    @Cacheable(value = "question",key="#id")
    public Question findQuestionById(Long id, Integer status){
        return questionDao.findQuestionById(id,status);
    }

    /**
     * 按id查询问题统计信息
     *
     * @param questionId
     *         questionId
     * @return
     */
    public QuestionCount findQuestionCountById(Long questionId){
        return questionDao.findQuestionCountById(questionId);
    }

    /**
     * 查询文章短域名是否存在
     *
     * @param shortUrl
     * @return
     */
    public boolean checkQuestionByShorturl(String shortUrl) {
        int totalCount = questionDao.checkQuestionByShorturl(shortUrl);
        return totalCount > 0 ? true : false;
    }

    public String shortUrl(){
        String[] aResult = ShortUrlUtils.shortUrl (null);
        String code=null;
        for ( int i = 0; i < aResult. length ; i++) {
            code=aResult[i];
            //查询问答短域名是否被占用
            if(!this.checkQuestionByShorturl(code)){
                break;
            }
        }
        return code;
    }

    /**
     * 按id和审核状态查询信息是否存在
     *
     * @param id
     *         产品id
     * @param status
     *         0所有，1未审核 2正常状态 3审核未通过 4删除
     * @return
     */
    public boolean checkQuestion(Long id,Integer status){
        return questionDao.findQuestionById(id, status) != null;
    }

    //按id删除角色和权限关联信息
    public boolean checkQuestionFollow(Long questionId,Long userId){
        int totalCount = questionDao.checkQuestionFollow(questionId,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询用户组名是否存在,如果id、userId不为空或者null，排除当前id意外检查是否已存在！
     *
     * @param title
     *         标题
     * @param userId
     *         用户id
     * @return
     */
    public boolean checkQuestionByTitle(String title,Long userId) {
        int totalCount = questionDao.checkQuestionByTitle(title,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 按id查询问题浏览数量统计
     *
     * @param title
     *         问题标题
     * @param userId
     *         用户id
     * @param createTime
     *         添加时间
     * @param status
     *         审核状态
     * @return
     */
    public int getQuestionCount(String title,Long userId,String createTime,Integer status){
        return questionDao.getQuestionCount(title,userId,createTime,status);
    }

    /**
     * 问题翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Question> getQuestionListPage(String title,Long userId,String createTime,Integer status,String orderby,String order, int pageNum, int rows) {
        PageVo<Question> pageVo = new PageVo<Question>(pageNum);
        pageVo.setRows(rows);
        List<Question> list = new ArrayList<Question>();
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(questionDao.getQuestionList(title,userId,createTime,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(questionDao.getQuestionCount(title,userId,createTime,status));
        return pageVo;
    }

    //查询信息总数
    public int getQuestionIndexCount(){
        return questionDao.getQuestionIndexCount();
    }

    //分享索引列表
    public List<Question> getQuestionIndexList(int pageNum, int rows){
        return questionDao.getQuestionIndexList(pageNum,rows);
    }
}
