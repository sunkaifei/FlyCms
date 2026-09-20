package com.flycms.module.user.dao;

import com.flycms.module.user.model.*;
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
 * @Date: 10:00 2018/7/9
 */
@Repository
public interface UserDao {

    // ///////////////////////////////
    // /////     增加         ////////
    // ///////////////////////////////
    //添加用户信息
    public int addUser(User user);

    //添加用户保持登录状态记录
    public int addUserSession(UserSession userSession);

    /**
     * 添加用户统计表
     *
     * @param userId
     *         用户id
     * @return
     */
    public int addUserCount(@Param("userId") long userId);

    /**
     * 添加用户关联信息
     *
     * @param userId
     *         用户id
     * @return
     */
    public int addUserAccount(@Param("userId") long userId);

    /**
     * 添加用户与权限组关联信息
     *
     * @param groupId
     *        权限组id
     * @param userId
     *        用户id
     * @return
     */
    public int addUserAndGroup(@Param("groupId") Long groupId,@Param("userId") Long userId);

    //添加用户注册验证操作记录
    public int addUserActivation(UserActivation userActivation);

    //添加用户关注操作记录
    public int addUserFans(UserFans userFans);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除用户信息
    public int deleteUserById(@Param("userId") Long userId);

    //按关注人id和粉丝ID删除用户粉丝关联信息
    public int deleteUserFans(@Param("userFollow") Long userFollow,@Param("userFans") Long userFans);

    /**
     * 按用户seeeionKey查询删除用户登陆保持记录
     *
     * @param sessionKey
     *         用户seeeionKey
     * @return
     */
    public int deleteUserSessionBySessionKey(@Param("sessionKey") String sessionKey);
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //更新用户登录ip地址和登录时间
    public int updateUserLogin(User user);

    //按id更新用户信息
    public int updateUser(User user);

    /**
     * 修改密码
     *
     * @param userId
     * @param password
     * @return Integer
     */
    public int updatePassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 更新统计用户所有提的问题数量
     *         问题数量
     * @param userId
     *         用户id
     * @return
     */
    public int updateQuestionCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有关注的问题数量
     *         问题数量
     * @param userId
     *         用户id
     * @return
     */
    public int updateQuestionFollowCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有发布文章数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateArticleCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有发布分享数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateShareCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有加入的话题数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateTopicCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有发布的答案数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateAnswerCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有关注人数数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateUserFollwCount(@Param("userId") Long userId);

    /**
     * 更新统计用户所有粉丝数量
     *
     * @param userId
     *         用户id
     * @return
     */
    public int updateUserFansCount(@Param("userId") Long userId);

    /**
     * 按用户名（邮箱、手机号）+ 验证码查询修改验证状态为已验证，0未验证，1为已验证
     *
     * @param userName
     *         需要查询的手机号码或者邮箱
     * @param code
     *         验证码
     * @return
     */
    public int updateUserActivationByStatus(@Param("userName") String userName,@Param("code") String code);

    /**
     * 修改用户头像
     *
     * @param userId
     * @param avatar
     * @return
     */
    public int updateAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 更新用户最终积分
     *
     * @param calculate
     *        运算：plus是加+,reduce是减-
     * @param score
     *        积分数量
     * @param userId
     *        用户id
     * @return
     */
    public int updateUserAccountScore(@Param("calculate") String calculate,@Param("score") Integer score,@Param("userId") Long userId);

    /**
     * 修改用户登录手机号码
     *
     * @param userMobile
     *         用于登录手机号码
     * @param userId
     *         用户id
     * @return
     */
    public int updateuUserMobile(@Param("userMobile") String userMobile,@Param("userId") Long userId);


    /**
     * 修改用户登录邮箱地址
     *
     * @param userEmail
     *         用于登录的邮箱地址
     * @param userId
     *         用户id
     * @return
     */
    public int updateuUserEmail(@Param("userEmail") String userEmail,@Param("userId") Long userId);

    /**
     * 更新用户权限组信息
     *
     * @param userRole
     *         用户实体类
     * @return
     */
    public int updateUserAndGroup(UserRole userRole);

    //添加用户保持登录状态记录
    public int updateUserSession(UserSession userSession);
    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    //按shortUrl查询用户信息
    public User findUserByShorturl(@Param("shortUrl") String shortUrl);

    /**
     * 通过userId查询用户信息
     * 用户状态0是所有 1未审核 2正常状态 3 删除至回收站 4锁定
     *
     * @param userId
     *         用户id
     * @param status
     *         审核状态
     * @return User
     */
    public User findUserById(@Param("userId") Long userId, @Param("status") Integer status);

    //按用户id查询用户统计信息
    public UserCount findUserCountById(@Param("userId") Long userId);

    //按用户id查询用户信息
    public UserAccount findUserAccountById(@Param("userId") Long userId);

    /**
     * 查询问答短域名是否被占用
     *
     * @param shortUrl
     * @return
     */
    public int checkUserByShorturl(@Param("shortUrl") String shortUrl);

    //按id查询用户是否存在
    public int checkUserById(@Param("userId") Long userId);

    /**
     * 通过username查询用户信息
     *
     * @param userName
     * @return User
     */
    public User findByUsername(@Param("userName") String userName);

    /**
     * 通过userEmail查询用户信息
     *
     * @param userEmail
     * @return User
     */
    public User findByEmail(@Param("userEmail") String userEmail);

    /**
     * 通过手机号码查询用户信息
     *
     * @param userMobile
     * @return User
     */
    public User findByMobile(@Param("userMobile") String userMobile);

    /**
     * 排除当前用户id后查询用户名是否存在,如果userId设置为null则查全部的用户名
     *
     * @param userName
     *         用户登录名
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public int checkUserByUserName(@Param("userName") String userName,@Param("userId") Long userId);

    /**
     * 排除当前用户id后查询手机号码是否存在,如果userId设置为null则查全部的手机号码
     *
     * @param userMobile
     *         用户登录手机号码
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public int checkUserByMobile(@Param("userMobile") String userMobile,@Param("userId") Long userId);

    /**
     * 排除当前用户id后查询当前邮箱是否存在,如果userId设置为null则查全部的邮箱
     *
     * @param userEmail
     *         用户登录邮箱地址
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public int checkUserByEmail(@Param("userEmail") String userEmail,@Param("userId") Long userId);

    /**
     * 排除当前用户id后查询当前昵称是否存在,如果userId设置为null则查全部的昵称
     *
     * @param nickName
     *         用户登录邮箱地址
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public int checkUserByNickName(@Param("nickName") String nickName,@Param("userId") Long userId);

    /**
     * 查询当前用户权限是否存在
     *
     * @param userId
     *         用户id
     * @return
     */
    public int checkUserByRole(@Param("userId") Long userId);

    /**
     * 查询当前用户是否是未激活权限组内用户
     *
     * @param userId
     *         用户id
     * @param groupId
     *         未激活权限组id
     * @return
     */
    public int checkUserByActivation(@Param("userId") Long userId,@Param("groupId") Long groupId);

    /**
     * 查询验证码在当前时间5分钟内获取并且是否过时或不存在
     *
     * @param userName
     *         查询的用户名
     * @param codeType
     *         查询的验证码类型，1手机注册验证码,2安全手机设置验证码,3密码重置验证码
     * @return
     */
    public UserActivation findByUserActivationCode(@Param("userName") String userName,@Param("codeType") Integer codeType);

    /**
     * 查询指定日期内申请验证码次数
     *
     * @param userName
     * @param referTime
     * @return
     */
    public int checkUserActivationCount(@Param("userName") String userName,@Param("referTime") String referTime);

    /**
     * 查询是否已关注或者是该用户粉丝
     *
     * @param userFollow
     *         关注者id
     * @param userFans
     *         粉丝id
     * @return
     */
    public int checkUserFans(@Param("userFollow") Long userFollow,@Param("userFans") Long userFans);

    /**
     * 查询是否是互相关注用户，等于2则为互相关注
     *
     * @param userFollow
     *         关注者id
     * @param userFans
     *         粉丝id
     * @return
     */
    public int checkUserMutualFans(@Param("userFollow") Long userFollow,@Param("userFans") Long userFans);

    //查询用户组总数
    public int getUserCount(@Param("userName") String userName,
                            @Param("trueName") String trueName,
                            @Param("userMobile") String userMobile,
                            @Param("userEmail") String userEmail);

    //用户组列表
    public List<User> getUserList(@Param("userName") String userName,
                                  @Param("trueName") String trueName,
                                  @Param("userMobile") String userMobile,
                                  @Param("userEmail") String userEmail,
                                  @Param("orderby") String orderby,
                                  @Param("order") String order,
                                  @Param("offset") int offset,
                                  @Param("rows") int rows);

    //查询粉丝总数
    public int getUserFansCount(@Param("userFollow") Long userFollow,
                            @Param("userFans") Long userFans,
                            @Param("createTime") String createTime);

    //粉丝列表
    public List<UserFans> getUserFansList(@Param("userFollow") Long userFollow,
                                  @Param("userFans") Long userFans,
                                  @Param("createTime") String createTime,
                                  @Param("orderby") String orderby,
                                  @Param("order") String order,
                                  @Param("offset") int offset,
                                  @Param("rows") int rows);

    //查询热门用户组总数
    public int getUserHotCount(@Param("userName") String userName,
                            @Param("nickName") String nickName,
                            @Param("userMobile") String userMobile,
                            @Param("userEmail") String userEmail,
                            @Param("province") Integer province,
                            @Param("city") Integer city,
                            @Param("area") Integer area,
                            @Param("status") Integer status);

    //查询热门用户组列表
    public List<User> getUserHotList(@Param("userName") String userName,
                                  @Param("nickName") String nickName,
                                  @Param("userMobile") String userMobile,
                                  @Param("userEmail") String userEmail,
                                  @Param("province") Integer province,
                                  @Param("city") Integer city,
                                  @Param("area") Integer area,
                                  @Param("status") Integer status,
                                  @Param("orderby") String orderby,
                                  @Param("order") String order,
                                  @Param("offset") int offset,
                                  @Param("rows") int rows);

    /**
     * 根据UserId查询用户登录时间
     *
     * @param sessionKey
     *         用户seeeionKey
     * @return
     */
    public UserSession findUserSessionBySeeeionKey(@Param("sessionKey") String sessionKey);


    /**
     * 查询当前用户登陆状态是否存在
     *
     * @param userId
     *         当前用户id
     * @return
     */
    public int checkUserSessionByUserId(@Param("userId") Long userId);

}
