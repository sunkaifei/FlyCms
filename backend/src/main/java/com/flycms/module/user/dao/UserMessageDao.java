package com.flycms.module.user.dao;

import com.flycms.module.user.model.UserMessage;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户站内信 DAO（2026-10-23）。
 *
 * <p>所有查询都以 user_id 打头走 {@code idx_msg_user}，未读数走覆盖索引不回表。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Repository
public interface UserMessageDao {

    int addMessage(UserMessage msg);

    UserMessage findById(@Param("id") Long id);

    /** 我的消息（isRead 为 null 时不过滤） */
    List<UserMessage> selectByUser(@Param("userId") Long userId,
                                   @Param("isRead") Integer isRead,
                                   @Param("offset") int offset,
                                   @Param("rows") int rows);

    int countByUser(@Param("userId") Long userId, @Param("isRead") Integer isRead);

    int countUnread(@Param("userId") Long userId);

    /** 标记已读：只更新自己名下的未读行，返回实际影响行数 */
    int markRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    int markAllRead(@Param("userId") Long userId);

    /** 删除自己的消息（用户侧无法删别人的） */
    int deleteByUser(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    // /////////////////// 后台管理侧 ///////////////////

    List<UserMessage> selectAdminPage(@Param("userId") Long userId,
                                      @Param("msgType") Integer msgType,
                                      @Param("isRead") Integer isRead,
                                      @Param("keyword") String keyword,
                                      @Param("offset") int offset,
                                      @Param("rows") int rows);

    int countAdminPage(@Param("userId") Long userId,
                       @Param("msgType") Integer msgType,
                       @Param("isRead") Integer isRead,
                       @Param("keyword") String keyword);

    /** 后台删除（不受 user_id 约束） */
    int deleteByIds(@Param("ids") List<Long> ids);

    /** 清理某个用户超过 keepDays 天的已读消息（保留策略） */
    int deleteReadBefore(@Param("days") int days);
}
