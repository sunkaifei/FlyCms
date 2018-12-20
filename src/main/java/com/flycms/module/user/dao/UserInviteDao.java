package com.flycms.module.user.dao;

import com.flycms.module.user.model.UserInvite;
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
 * @Date: 10:08 2018/8/217
 */
@Repository
public interface UserInviteDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // //////////////////////////////
    //添加用户邀请信息记录
    public int addUserInvite(UserInvite invite);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    public UserInvite findInviteByid(@Param("id") Long id);

    /**
     * 查询当前用户id对邀请用户id是否存在
     *
     * @param toUserId
     *         被邀请人ID
     * @param formUserId
     *         邀请人ID
     * @return
     */
    public int checkUserInvite(@Param("toUserId") Long toUserId,@Param("formUserId") Long formUserId);

    //查询用户组总数
    public int getUserInviteCount(@Param("userId") Long userId, @Param("status") Integer status);

    //用户组列表
    public List<UserInvite> getUserInviteList(@Param("userId") Long userId,
                                        @Param("status") Integer status,
                                        @Param("orderby") String orderby,
                                        @Param("order") String order,
                                        @Param("offset") int offset,
                                        @Param("rows") int rows);
}
