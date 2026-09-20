package com.flycms.module.message.dao;

import com.flycms.module.message.model.Message;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 15:56 2018/11/13
 */
@Repository
public interface MessageDao {

    // ///////////////////////////////
    // ///// 增加 ////////
    // ///////////////////////////////
    /**
     * 插入站内短信操作
     *
     * @param message
     *
     * @return
     */
    public int addMessage(Message message);


    // ///////////////////////////////
    // ///// 刪除 ////////
    // ///////////////////////////////

    /**
     * 按id删除站内短信信息
     *
     * @param id
     * @return
     */
    public boolean deleteMessageById(@Param("id") long id);


    // ///////////////////////////////
    // ///// 修改 ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    /**
     * 按id和所属用户id查询站内短信信息
     *
     * @param id
     *        短信id
     * @return
     */
    public Message findMessageById(@Param("id") Integer id);

    /**
     * 按用户id查询最后对话数
     *
     * @param fromId
     * @param toId
     * @param subject
     * @param sendTime
     * @param writeTime
     * @param hasView
     * @param isAdmin
     * @param state
     * @return
     */
    public int getMessageCount(
            @Param("fromId") Integer fromId,
            @Param("toId") Integer toId,
            @Param("subject") String subject,
            @Param("sendTime") String sendTime,
            @Param("writeTime") String writeTime,
            @Param("hasView") Integer hasView,
            @Param("isAdmin") Integer isAdmin,
            @Param("state") Integer state
    );

    /**
     * 按用户id查询最后对话列表
     *
     * @param fromId
     * @param toId
     * @param subject
     * @param sendTime
     * @param writeTime
     * @param hasView
     * @param isAdmin
     * @param state
     * @param orderby
     * @param order
     * @param offset
     * @param rows
     * @return
     */
    public List<Message> getMessageList(
            @Param("fromId") Integer fromId,
            @Param("toId") Integer toId,
            @Param("subject") String subject,
            @Param("sendTime") String sendTime,
            @Param("writeTime") String writeTime,
            @Param("hasView") Integer hasView,
            @Param("isAdmin") Integer isAdmin,
            @Param("state") Integer state,
            @Param("orderby") String orderby,
            @Param("order") String order,
            @Param("offset") int offset,
            @Param("rows") int rows);

}

