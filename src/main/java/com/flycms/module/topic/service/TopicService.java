/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */

package com.flycms.module.topic.service;

import java.util.Date;
import java.util.List;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.ShortUrlUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.topic.dao.TopicDao;
import com.flycms.module.topic.model.Topic;
import com.flycms.module.topic.model.TopicEdit;
import com.flycms.module.topic.model.TopicInfo;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;


/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 话题服务
 * 
 * @author sunkaifei
 * 
 */
@Service
public class TopicService {
	
	@Autowired
	private TopicDao topicDao;
	@Autowired
	protected UserService userService;
	
	// ///////////////////////////////
	// ///// 增加 ////////
	// ///////////////////////////////

	@Transactional
	public DataVo addTopic(Topic topic) {
		DataVo data = DataVo.failure("操作失败");
		if(this.checkTopicByTopic(topic.getTopic())){
			return data=DataVo.failure("该话题已存在");
		}
		SnowFlake snowFlake = new SnowFlake(2, 3);
		topic.setId(snowFlake.nextId());
		if(topic.getShortUrl()==null){
			String code=this.shortUrl();
			topic.setShortUrl(code);
		}
		topic.setCreateTime(new Date());
		topicDao.addTopic(topic);
		data = DataVo.jump("添加成功成功！","/system/topic/add");
		return data;
	}
	/**
	 * 增加话题信息
	 * 
	 * @param topics
	 *         话题
	 * @param content
	 *         话题介绍
	 * @param countView
	 *         初始浏览量
	 * @param countNum
	 *         初始浏览量
	 * @param isgood
	 * @param status
	 * @return
	 */
	@Transactional
	public Topic addTopic(String topics, String content, Integer countView, Integer countNum, Integer isgood, Integer status) {
		Topic topic = new Topic();
		SnowFlake snowFlake = new SnowFlake(2, 3);
		topic.setId(snowFlake.nextId());
		String code=this.shortUrl();
		topic.setShortUrl(code);
		topic.setTopic(topics);
		topic.setContent(content);
		topic.setCountView(countView);
		topic.setCountNum(countNum);
		topic.setIsgood(isgood);
		topic.setStatus(status);
        topic.setCreateTime(new Date());
		topicDao.addTopic(topic);
		return topic;
	}

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
	@Transactional
	public int addTopicAndInfo(Long infoId,Long topicId,Integer infoType,Integer status){
		SnowFlake snowFlake = new SnowFlake(2, 3);
		return topicDao.addTopicAndInfo(snowFlake.nextId(),infoId,topicId,infoType,status);
	}
	
	/**
	 * 添加用户关注的话题id
	 * 
	 * @param userId
	 *        用户id
	 * @param topicId
	 *        话题id
	 */
    @Transactional
	public DataVo addTopicAndUser(Long userId, Long topicId) {
        DataVo data = DataVo.failure("请勿非法传递参数");
		Topic topic = topicDao.findTopicById(topicId,2);
		if(topic==null){
			return data=DataVo.failure("该话题未审核或者不存在!");
		}
        if(checkTopicByUserId(userId,topicId)){
            topicDao.deleteTopicbyUserId(userId,topicId);
            topicDao.updateTopicFollowByCount(topicId);
			userService.updateTopicCount(userId);
            return data=DataVo.failure(2,"已取消");
        }else{
            topicDao.addTopicAndUser(userId,topicId,new Date());
            topicDao.updateTopicFollowByCount(topicId);
			userService.updateTopicCount(userId);
            return data = DataVo.success("已关注");
        }
	}

	////用户增加编辑话题信息
    @Transactional
    public DataVo addUserEditTopic(TopicEdit topicEdit){
        DataVo data = DataVo.failure("请勿非法传递参数");
        topicEdit.setStatus(0);
        topicEdit.setCreateTime(new Date());
        topicDao.addUserEditTopic(topicEdit);
        return data = DataVo.jump("话题内容已提交，等待审核！","/topics/"+topicEdit.getTopicId());
    }
	
	// ///////////////////////////////
	// ///// 刪除 ////////
	// ///////////////////////////////
	/**
	 * 按id删除标签信息
	 * 
	 * @param id
	 * @return
	 */
	public int deleteTagsById(Integer id) {
		return topicDao.deleteTopicById(id);
	}
	

	
	/**
	 * 按文章id查找标签更新标签关联文章数量，删除标签和文章关联信息
	 * 
	 * @param infoType
     *         信息类型
     * @param infoId
     *         信息id
	 * @return
	 */
	public void deleteTopicAndInfoUpCount(Integer infoType,Long infoId){
		List<Topic> taglist=this.getInfoByTopicList(infoType,infoId);
		if(taglist.size()>0){
			for (Topic list : taglist) {
				topicDao.deleteTopicAndInfo(infoId,list.getId(),infoType);
				this.updateTopicByCount(list.getId());
			}
		}	
	}

	// ///////////////////////////////
	// ///// 修改 ////////
	// ///////////////////////////////
	/**
	 * 按id更新标签信息
	 * 
	 * @param topics
	 * @param content
	 * @param countView
	 * @param isgood
	 * @param status
	 * @param id
	 * @return
	 */
	public Topic updateTopicById(String topics,String content,Integer countView,Integer isgood,Integer status,Long id) {
		Topic topic = new Topic();
		topic.setTopic(topics);
		topic.setContent(content);
		topic.setCountView(countView);
		topic.setIsgood(isgood);
		topic.setStatus(status);
		topic.setId(id);
		topicDao.updateTopicById(topic);
		return topic;
	}
	
	/**
	 * 按id更新标签统计信息
	 *
	 * @param id
	 * @return
	 */
	public int updateTopicByCount(Long id){
		return topicDao.updateTopicByCount(id);
	}
	
	/**
	 * 按id更新审核状态
	 * 
	 * @param status
	 * @param id
	 * @return
	 */
	public int updateTagStatus(Integer status,Long id) {
		return topicDao.updateTagStatus(status,id);
	}
	
	// ///////////////////////////////
	// ///// 查询 ////////
	// ///////////////////////////////
	public Topic findTopicByShorturl(String shortUrl){
		return topicDao.findTopicByShorturl(shortUrl);
	}
	/**
	 * 按id查询标签信息
	 * 
	 * 按id查询标签
	 * 
	 * @param id
	 *        标签id
	 * @param status
	 *        审核状态：0所有状态，1未审核，2审核
	 * @return
	 */
	public Topic findTopicById(Long id,Integer status){
		return topicDao.findTopicById(id,status);
	}

	/**
	 * 按话题查询该话题信息
	 *
	 * @param topic
	 *         话题
	 * @return
	 */
	public Topic findTopicByTopic(String topic){
		return topicDao.findTopicByTopic(topic);
	}

	/**
	 * 查询话题短域名是否被占用
	 *
	 * @param shortUrl
	 * @return
	 */
	public boolean checkTopicByShorturl(String shortUrl) {
		int totalCount = topicDao.checkTopicByShorturl(shortUrl);
		return totalCount > 0 ? true : false;
	}

	public String shortUrl(){
		String[] aResult = ShortUrlUtils.shortUrl (null);
		String code=null;
		for ( int i = 0; i < aResult. length ; i++) {
			code=aResult[i];
			//查询文章短域名是否被占用
			if(!this.checkTopicByShorturl(code)){
				break;
			}
		}
		return code;
	}

	//查询话题是否存在
	public boolean checkTopicByTopic(String topic) {
		int totalCount = topicDao.checkTopicByTopic(topic);
		return totalCount > 0 ? true : false;
	}

	//查询用户下是否该关注标签
    public boolean checkTopicByUserId(Long userId,Long topicId) {
        int totalCount = topicDao.checkTopicByUserId(userId,topicId);
        return totalCount > 0 ? true : false;
    }

	/**
	 * 查标签翻页列表
	 *
	 * @param topic
	 *        分类ID
	 * @param type
	 *        标签
	 * @param isgood
	 *        推荐设置
	 * @param status
	 *        审核状态，1启用，0是停止
	 * @param orderby
	 *        排序规则，可指定排序count_view（标签浏览量）、count_num（标签被文章引用数量）、time（更新时间）、article_id（文章id）
	 * @param order
	 *        排序方式，asc升序，desc是降序
	 * @param pageNum
	 *        当前页码数
	 * @param rows
	 *        当前列表页记录条数
	 * @return
	 */
	public PageVo<Topic> getTopicListPage(String topic,String type,Integer isgood,Integer status, String orderby, String order, Integer pageNum, Integer rows){
		PageVo<Topic> pageVo = new PageVo<Topic>(pageNum);
		pageVo.setRows(rows);

        String field=null;
		if("hot".equals(orderby) || "click".equals(orderby)){
            field="count_view";
		}else if("sortrank".equals(orderby) || "pubdate".equals(orderby)){
            field="add_time";
		}else{
            field="id";
        }
		if(order==null){
			order="desc";
		}
		List<Topic> list =null;
		if(!"rand".equals(orderby)){
		    list = topicDao.getTopicList(topic,type,isgood,status,field,order,pageVo.getOffset(), pageVo.getRows());
		}else{
		    //随机列表
		    list = topicDao.getTopicRandList(topic,type,isgood,status,field,order,pageVo.getOffset(), pageVo.getRows());
		}
        int count = topicDao.getTopicCount(topic,type,isgood,status);
		pageVo.setList(list);
		pageVo.setCount(count);
		return pageVo;
	}
	
	/**
	 * 查询所有tag信息
	 * 
	 * @return
	 */
	public List<Topic> allWord(){
		List<Topic> tags = topicDao.allWord();
		return tags;
	}

	//话题id查询关联的内容
    public PageVo<TopicInfo> getTopicAndInfoListPage(Integer infoType,Long topicId,Integer status,String orderBy, String order,Integer pageNum, Integer rows){
        PageVo<TopicInfo> pageVo = new PageVo<TopicInfo>(pageNum);
        pageVo.setRows(rows);
        if(orderBy==null){
            orderBy="id";
        }
        if(order==null){
            order="desc";
        }
        List<TopicInfo>	list = topicDao.getTopicAndInfoList(infoType,topicId,status,orderBy,order,pageVo.getOffset(), pageVo.getRows());
        int count = topicDao.getTopicAndInfoCount(infoType,topicId,status);
        pageVo.setList(list);
        pageVo.setCount(count);
        return pageVo;
    }
	
	/**
	 * 按文章id查询所有文章关联的标签
	 * 
	 * @param infoId
	 *        文章id
	 * @return
	 */
	public List<Topic> getInfoByTopicList(Integer infoType,long infoId){
		return topicDao.getInfoByTopicList(infoType,infoId);
	}

	public PageVo<Topic> getUserTagsListPage(Integer user_id,Integer pageNum, Integer rows){
		PageVo<Topic> pageVo = new PageVo<Topic>(pageNum);
		pageVo.setRows(rows);
        List<Topic>	list = topicDao.getUserTagsList(user_id,pageVo.getOffset(), pageVo.getRows());
        int count = topicDao.getUserTagsCount(user_id);
		pageVo.setList(list);
		pageVo.setCount(count);
		return pageVo;
	}

}
