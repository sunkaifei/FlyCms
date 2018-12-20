package com.flycms.module.message.service;

import java.text.ParseException;
import java.util.List;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.message.dao.MessageDao;
import com.flycms.module.message.model.Message;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 站内短信服务
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:12 2018/11/14
 */
@Slf4j
@Service
public class MessageService {
	
	@Autowired
	private MessageDao messageDao;
	
	@Autowired
	protected UserService userService;
	
	// ///////////////////////////////
	// ///// 增加 ////////
	// ///////////////////////////////		

	/**
	 * 撰写站内短信
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public DataVo addMessage(Message message) {
		DataVo data = DataVo.failure("操作失败");
		//格式化系统时间
		SnowFlake snowFlake = new SnowFlake(2, 3);
		message.setId(snowFlake.nextId());
		int totalCount = messageDao.addMessage(message);
		if(totalCount > 0){
			data = DataVo.success("信息已发送！", DataVo.NOOP);
		}else{
			data = DataVo.failure("发送失败，请联系管理员！");
		}
		return data;
	}
	
	// ///////////////////////////////
	// ///// 刪除 ////////
	// ///////////////////////////////
	/**
	 * 按id删除关于我们信息
	 * 
	 * @param aboutId
	 * @return
	 */
	public boolean deleteMessageById(long aboutId) {
		return messageDao.deleteMessageById(aboutId);
	}
	
	// ///////////////////////////////
	// ///// 修改 ////////
	// ///////////////////////////////

	
	// ///////////////////////////////
	// ///// 查询 ////////
	// ///////////////////////////////
	/**
	 * 按id和所属用户id查询站内短信信息
	 * 
	 * @param id
	 *        短信id
	 * @return
	 */
	public Message findMessageById(Integer id){
		return messageDao.findMessageById(id);
	}

	/**
	 * feed翻页列表
	 *
	 *        0、所有，1查询属性是0的，2，查询属性为1的，3查询属性为2的
	 * @param pageNum
	 *        翻页数
	 * @param rows
	 *        每页条数
	 * @return
	 * @throws Exception 
	 */
	@Cacheable(value = "message")
	public PageVo<Message> getMessageListPage(Integer fromId, Integer toId, String subject, String sendTime, String writeTime, Integer hasView, Integer isAdmin, Integer state, String orderby, String order, int pageNum, int rows) throws Exception, ParseException {
		PageVo<Message> pageVo = new PageVo<Message>(pageNum);
		pageVo.setRows(rows);
		if(orderby==null){
			orderby="send_time";
		}
		if(order==null){
			order="desc";
		}
		pageVo.setCount(messageDao.getMessageCount(fromId,toId,subject,sendTime,writeTime,hasView,isAdmin,state));
		List<Message> messagelist = messageDao.getMessageList(fromId,toId,subject,sendTime,writeTime,hasView,isAdmin,state,orderby,order,pageVo.getOffset(), pageVo.getRows());
		
				for (Message message:messagelist) {
					//发件人信息
					User fromuser=userService.findUserById(message.getFromId(),0);
					String fromface=fromuser.getAvatar();
					if("".equals(fromface) || fromface==null){
						fromface="/assets/images/ava/default.png";
					}
					//收件人信息
					User touser=userService.findUserById(message.getToId(),0);
					String toface=touser.getAvatar();
					if("".equals(toface) || toface==null){
						toface="/assets/images/ava/default.png";
					}
					message.setFromFace(fromface);
					message.setFromNickname(fromuser.getNickName());
					message.setToFace(toface);
					message.setToNickname(touser.getNickName());
				}
		pageVo.setList(messagelist);
		return pageVo;
	}
}
