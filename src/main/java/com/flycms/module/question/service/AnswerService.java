package com.flycms.module.question.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.question.dao.AnswerDao;
import com.flycms.module.question.dao.QuestionDao;
import com.flycms.module.question.model.Answer;
import com.flycms.module.question.model.Question;
import com.flycms.module.user.service.FeedService;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 答案处理服务
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 23:51 2018/8/22
 */
@Service
public class AnswerService {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerDao answerDao;
    @Autowired
    protected UserService userService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private QuestionDao questionDao;
    @Autowired
    protected ImagesService imagesService;
    @Autowired
    protected FeedService feedService;

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////


    //添加用户答案
    @Transactional
    public DataVo addAnswer(Long questionId,Long userId,String content) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        if(checkAnswerByContent(userId,content)){
            return data= DataVo.failure("请勿重复发表相同内容");
        }
        Answer answer=new Answer();
        SnowFlake snowFlake = new SnowFlake(2, 3);
        answer.setId(snowFlake.nextId());
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setContent(imagesService.replaceContent(1,answer.getId(),answer.getUserId(),content));
        answer.setCreateTime(new Date());
        answer.setStatus(Integer.parseInt(configService.getStringByKey("user_answer_verify")));
        int totalCount=answerDao.addAnswer(answer);
        if(totalCount>0){
            if(answer.getStatus()==1) {
                //添加用户feed信息
                feedService.addUserFeed(userId, 3, answer.getId());
                //更新用户发布答案数量
                userService.updateAnswerCount(userId);
                //更新回答数量统计
                questionDao.updateQuestionByAnswerCount(questionId);

            }
            //添加答案统计信息
            answerDao.addAnswerCount(answer.getId());
            data=DataVo.success("操作成功", DataVo.NOOP);
        }else{
            return data= DataVo.failure("添加失败，未知原因，请联系管理员！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按答案id删除答案信息
    @Transactional
    public DataVo deleteAnswerById(Long id){
        DataVo data = DataVo.failure("操作失败");
        Answer answer=answerDao.findAnswerById(id,0);
        if(answer!=null){
            //更新回答数量统计
            questionDao.updateQuestionByAnswerCount(answer.getQuestionId());
            //查询该Feed答案修改审核状态，设置为影藏
            feedService.updateuUserFeedById(3,answer.getId(),0);
            //更新用户发布答案数量
            userService.updateAnswerCount(answer.getUserId());
            //答案id删除该id信息
            answerDao.deleteAnswerById(id);
            //按答案id删除答案相关统计信息
            answerDao.deleteAnswerCountById(answer.getId());
            data=DataVo.success("已删除");
        }else{
            return data= DataVo.failure("答案内容不存在");
        }
        return data;
    }

    //执行删除问题时同时删除关联的答案内容
    @Transactional
    public boolean deleteQuestionAndAnswerById(Long id){
        Answer answer=answerDao.findAnswerById(id,0);
        if(answer!=null){
            //答案内内容里的图片未做删除处理，这里备忘！

            //更新统计用户所有发布的答案数量
            userService.updateAnswerCount(answer.getUserId());
            answerDao.deleteAnswerById(id);
            return true;
        }
        return false;
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 按id更新文章审核状态
     *
     * @param id
     *         问题id
     * @param status
     *         0未审核 1正常状态 2审核未通过 3删除
     * @return
     */
    @Transactional
    public DataVo updateAnswerStatusById(Long id, Integer status) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Answer answer=answerDao.findAnswerById(id,0);
        if(answer==null){
            return data = DataVo.failure("该信息不存在或已删除");
        }
        answerDao.updateAnswerStatusById(id,status);
        if(status == 1){
            //添加用户feed信息,如果存在在修改状态为1
            if(feedService.checkUserFeed(answer.getUserId(),3,answer.getId())){
                feedService.addUserFeed(answer.getUserId(),3,answer.getId());
            }else{
                feedService.updateuUserFeedById(3,answer.getId(),1);
            }
            userService.updateAnswerCount(answer.getUserId());
            //更新回答数量统计
            questionDao.updateQuestionByAnswerCount(answer.getQuestionId());
        }else{
            //删除用户feed信息
            feedService.updateuUserFeedById(3,answer.getId(),0);
            //更新用户问答数量
            userService.updateAnswerCount(answer.getUserId());
            //更新回答数量统计
            questionDao.updateQuestionByAnswerCount(answer.getQuestionId());
        }
        data=DataVo.success("审核操作成功！");
        return data;
    }

    @Transactional
    public DataVo updateAnswerById(Long id,Long userId, String content) throws Exception {
        DataVo data = DataVo.failure("该信息不存在或已删除");
        Answer answer=this.findAnswerByIdAndUserId(id,userId);
        if(answer==null){
            return data = DataVo.failure("该答案不存在或已删除");
        }
        answer.setContent(imagesService.replaceContent(1,answer.getId(),answer.getUserId(),answer.getContent()));
        answer.setLastTime(new Date());
        Question question=questionService.findQuestionById(answer.getQuestionId(),0);
        if(question.getStatus()!=1){
            answer.setStatus(0);
        }else{
            answer.setStatus(Integer.parseInt(configService.getStringByKey("user_answer_verify")));
        }
        answerDao.updateAnswerById(answer);
        userService.updateAnswerCount(answer.getUserId());
        //更新回答数量统计
        questionDao.updateQuestionByAnswerCount(answer.getQuestionId());
        data=DataVo.jump("答案修改成功！","/question/"+question.getId());
        return data;
    }

    // ///////////////////////////////
    // /////        查询      ////////
    // ///////////////////////////////
    //按id查询答案信息
    public Answer findAnswerById(Long id,Integer status){
        return answerDao.findAnswerById(id,status);
    }

    /**
     * 按id和用户id查询评论内容
     *
     * @param id
     *         答案id
     * @param userId
     *         用户id
     * @return
     */
    public Answer findAnswerByIdAndUserId(Long id,Long userId){
        return answerDao.findAnswerByIdAndUserId(id,userId);
    }

    /**
     * 查询该用户同样答案内容是否已存在！
     *
     * @param userId
     *         用户id
     * @param content
     *         答案内容
     * @return
     */
    public boolean checkAnswerByContent(Long userId,String content) {
        int totalCount = answerDao.checkAnswerByContent(userId,content);
        return totalCount > 0 ? true : false;
    }

    /**
     * 答案列表翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Answer> getAnswerListPage(Long questionId, Long userId, String addTime, Integer status,String orderby,String order, int pageNum, int rows) {
        PageVo<Answer> pageVo = new PageVo<Answer>(pageNum);
        pageVo.setRows(rows);
        List<Answer> list = new ArrayList<Answer>();
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(answerDao.getAnswerList(questionId,userId,addTime,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(answerDao.getAnswerCount(questionId,userId,addTime,status));
        return pageVo;
    }

    //按问题id或者用户id查询答案列表
    public List<Answer> gettAnswerByQuestionIdList(Long questionId, Long userId){
        return answerDao.gettAnswerByQuestionIdList(questionId,userId);
    }

    //按问题id查询最新的第一条评论内容
    public Answer findNewestAnswerById(Long questionId){
        return answerDao.findNewestAnswerById(questionId);
    }
}
