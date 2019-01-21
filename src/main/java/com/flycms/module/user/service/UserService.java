package com.flycms.module.user.service;

import com.flycms.constant.Const;
import com.flycms.constant.SiteConst;
import com.flycms.core.utils.*;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.other.service.EmailService;
import com.flycms.module.score.service.ScoreRuleService;
import com.flycms.module.user.dao.UserDao;
import com.flycms.module.user.model.User;
import com.flycms.module.config.service.SmsapiService;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.user.model.*;
import com.flycms.module.user.utils.UserSessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.*;
/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:14 2018/7/9
 */
@Slf4j
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserInviteService userInviteService;

    @Autowired
    private SmsapiService smsapiService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ScoreRuleService scoreRuleService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserSessionUtils userSessionUtils;

    @Autowired
    private SiteConst siteConst;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 用户注册基本信息
     *
     * @param userType
     *        用户注册类型，1：手机注册，2：邮箱注册，3：用户名注册
     * @param userName
     *        用户注册登录名
     * @param password
     *        用户注册密码
     * @param invite
     *        邀请用户id
     * @param request
     * @param response
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo addUserReg(int userType,String userName, String password, String code, String invite, HttpServletRequest request,HttpServletResponse response) throws Exception {
        DataVo data = DataVo.failure("请勿非法传递参数");
        if(Integer.parseInt(configService.getStringByKey("user_reg"))==0){
            return data =DataVo.failure("本站停止注册！如有需要请联系管理员！");
        }
        if(userType==1 || userType==3){
            //手机号码注册
            if(this.checkUserByMobile(userName,null) || this.checkUserByUsername(userName)){
                return data =DataVo.failure("已被手机号码或者用户名占用！");
            }
        }else if(userType==2){
            //邮箱注册
            if(this.checkUserByEmail(userName,null)){
                return data =DataVo.failure("该邮箱以被占用！");
            }
        }
        if(userType<3 && userType >0){
            if (this.checkUserActivationCode(userName,1,code)) {
                //验证通过后修改本条记录为已验证
                userDao.updateUserActivationByStatus(userName,code);
            }else {
                return DataVo.failure("手机验证码错误");
            }
        }
        User user = new User();
        if(userType==1){
            //手机号码注册
            user.setUserMobile(userName);
        }else if(userType==2){
            //邮箱注册
            user.setUserEmail(userName);
        }else if(userType==3){
            //用户名注册
            user.setUserName(userName);
        }else{
            return data;
        }
        SnowFlake snowFlake = new SnowFlake(2, 3);
        user.setUserId(snowFlake.nextId());
        String urlCode=this.shortUrl();
        user.setShortUrl(urlCode);
        user.setPassword(BCryptUtils.hashpw(password, BCryptUtils.gensalt()));
        user.setCreateTime(new Date());
        user.setLastLogin(new Date());
        //后台设置是否需要审核
        user.setStatus(Integer.parseInt(configService.getStringByKey("user_reg_verify")));
        //添加用户账号和密码
        userDao.addUser(user);
        //添加用户组权限
        userDao.addUserAndGroup(Long.parseLong(configService.getStringByKey("user_activation_role")),user.getUserId());
        if(invite==null){
            invite=CookieUtils.getCookie(request,"invite");
        }
        //添加邀请和被邀请用户关联信息
        if (NumberUtils.isNumber(invite)) {
            if(this.checkUserById(Long.parseLong(invite))){
                userInviteService.addUserInvite(user.getUserId(),Long.parseLong(invite));
                // 邀请奖励
                scoreRuleService.scoreRuleBonus(Long.parseLong(invite), 2L,user.getUserId());
            }
        }
        //添加用户统计表
        userDao.addUserCount(user.getUserId());
        //添加用户关联信息
        userDao.addUserAccount(user.getUserId());
        userSessionUtils.setLoginMember(request,response,false,user);
        return DataVo.success("用户注册成功");
    }

    //添加新用户信息
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo adminAddUser(User user){
        DataVo data = DataVo.failure("操作失败");
        if(user.getUserName()==null){
            return data=DataVo.success("用户名不能为空！");
        }
        if(user.getPassword()!=null){
            if(!user.getPassword().equals(user.getRepassword())){
                return data=DataVo.success("两次密码不一样");
            }
        }else{
            return data=DataVo.success("新用户密码不能为空！");
        }
        //用户添加时间
        String code=this.shortUrl();
        user.setShortUrl(code);
        user.setCreateTime(new Date());
        int totalCount = userDao.addUser(user);
        if(totalCount > 0){
            data = DataVo.success("新用户添加成功");
        }else{
            data=DataVo.failure("新用户添加失败！");
        }
        return data;
    }

    /**
     * 用户手机注册账号申请获取验证码
     *
     * @param phoneNumber
     *        手机号码
     * @return
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo regMobileCode(String phoneNumber) throws Exception {
        if(this.checkUserByMobile(phoneNumber,null)) {
            return DataVo.failure("该手机号码以注册或被占用！");
        }
        int count=userDao.checkUserActivationCount(phoneNumber,DateUtils.getDay());
        if(count<=5) {
            smsapiService.mobileRegisterCode(phoneNumber, MathUtils.getRandomCode(6),1);
        }else {
            return DataVo.failure("您今日已超出申请次数！");
        }
        return DataVo.success("验证码已发送，请查收！", DataVo.NOOP);
    }

    /**
     * 用户手机找回密码申请获取验证码
     *
     * @param mobile
     *        手机号码
     * @return
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo userGetBackCode(String mobile) throws Exception {
        if(this.findByMobile(mobile)) {
            return DataVo.failure("账号不存在或者未绑定手机号！");
        }
        int count=userDao.checkUserActivationCount(mobile,DateUtils.getDay());
        if(count<=5) {
            smsapiService.mobileRegisterCode(mobile, MathUtils.getRandomCode(6),3);
        }else {
            return DataVo.failure("您今日已超出申请次数！");
        }
        return DataVo.success("验证码已发送，请查收！", DataVo.NOOP);
    }

    @Transactional
    @CacheEvict(value="user", allEntries=true)
    public DataVo addUserFans(Long userFollow,Long userFans){
        DataVo data = DataVo.failure("操作失败");
        User user=userDao.findUserById(userFollow,2);
        if(user==null){
            return data=DataVo.failure("用户不存在或者以被禁用！");
        }
        if(this.checkUserFans(userFollow,userFans)){
            userDao.deleteUserFans(userFollow,userFans);
            userDao.updateUserFollwCount(userFans);
            userDao.updateUserFansCount(userFollow);
            return data=DataVo.failure(2,"已取消关注");
        }else{
            UserFans fans=new UserFans();
            SnowFlake snowFlake = new SnowFlake(2, 3);
            fans.setId(snowFlake.nextId());
            fans.setUserFollow(userFollow);
            fans.setUserFans(userFans);
            fans.setCreateTime(new Date());
            userDao.addUserFans(fans);
            userDao.updateUserFollwCount(userFans);
            userDao.updateUserFansCount(userFollow);
            return data = DataVo.success("已关注");
        }
    }

    //添加用户保持登录状态记录
    public int addUserSession(UserSession userSession) {
        return userDao.addUserSession(userSession);
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除用户信息
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo deleteUserById(Long userId){
        DataVo data = DataVo.failure("操作失败");
        int totalCount = userDao.deleteUserById(userId);
        if(totalCount > 0){
            data = DataVo.success("用户信息修改");
        }else{
            data=DataVo.failure("更新失败，请联系管理员！");
        }
        return data;
    }

    //按Sessionid查询删除用户登陆保持记录
    public void signOutLogin(HttpServletRequest request,HttpServletResponse response) {
        if(request.getSession()!=null){
            User userLogin = (User) request.getSession().getAttribute(Const.SESSION_USER);
            if(userLogin !=null && userLogin.getSessionKey()!=null){
                userDao.deleteUserSessionBySessionKey(userLogin.getSessionKey());
                //清除session
                request.getSession().removeAttribute(Const.SESSION_USER);//我这里是先取出httpsession中的user属性
                request.getSession().invalidate();  //然后是让httpsession失效
            }
        }
        //清除cookie
        try
        {
            Cookie cookie = new Cookie(siteConst.getSessionKey(), null);
            cookie.setMaxAge(0);
        }catch(Exception ex){
            log.error("清空Cookies发生异常！",ex);
        }
    }
    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 用户登陆
     *
     * @param username
     *        用户名
     * @param password
     *        密码
     * @param request
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public User userLogin(String username, String password,boolean keepLogin,HttpServletRequest request,HttpServletResponse response)throws Exception {
        User user = null;
        if(StringHelperUtils.checkPhoneNumber(username)) {
            user = userDao.findByMobile(username);
            if(user==null){
                user = userDao.findByUsername(username);
            }
        }else if(StringHelperUtils.emailFormat(username)){
            user = userDao.findByEmail(username);
        }else{
            user = userDao.findByUsername(username);
        }

        if (user != null) {
            User login = new User();
            if (BCryptUtils.checkpw(password, user.getPassword())) {
                login.setUserId(user.getUserId());
                login.setAttempts(0);
                login.setLastLogin(new Date());
                login.setLoginIp(IpUtils.getIpAddr(request));
                userDao.updateUserLogin(login);
                //用户信息写入session
                userSessionUtils.setLoginMember(request,response,keepLogin,user);
                //登录奖励
                scoreRuleService.scoreRuleBonus(user.getUserId(), 1L,user.getUserId());
            }else{
                login.setUserId(user.getUserId());
                login.setAttempts(user.getAttempts()+1);
                login.setAttemptsTime(new Date());
                userDao.updateUserLogin(login);
                user = null;
            }
        }
        return user;
    }

    //更新用户信息
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updateUser(User user){
        DataVo data = DataVo.failure("操作失败");
        if(user.getUserId()==null){
            return data=DataVo.failure("用户id不能为空！");
        }
        User userinfo=userDao.findUserById(user.getUserId(),0);
        if(userinfo==null){
            return data=DataVo.failure("用户不存在！");
        }
        if(!StringUtils.isBlank(user.getUserName())){
            if(!StringHelperUtils.checkUserName(user.getUserName())){
                return DataVo.failure("由字母数字下划线组成且开头必须是字母，不能超过16位！");
            }
            if(this.checkUserByUserName(user.getUserName(),user.getUserId())){
                return DataVo.failure("该用户名已注册或被占用！");
            }
        }
        if(!StringUtils.isBlank(user.getUserMobile())){
            if(!StringHelperUtils.checkPhoneNumber(user.getUserMobile())) {
                return DataVo.failure("手机号码错误！");
            }
            if(this.checkUserByMobile(user.getUserMobile(),user.getUserId())){
                return DataVo.failure("该手机号码已注册或被占用！");
            }
        }
        if(!StringUtils.isBlank(user.getUserEmail())){
            if(!StringHelperUtils.emailFormat(user.getUserEmail())) {
                return DataVo.failure("邮箱地址错误！");
            }
            if(this.checkUserByEmail(user.getUserEmail(),user.getUserId())){
                return DataVo.failure("该邮箱已注册或被占用！");
            }
        }
        if(!StringUtils.isBlank(user.getPassword())){
            if(!StringHelperUtils.IsPassword(user.getPassword())){
                return DataVo.failure("密码格式错误，不能有特殊符号！");
            }
            String pwHash = BCryptUtils.hashpw(user.getPassword(), BCryptUtils.gensalt());
            userDao.updatePassword(user.getUserId(),pwHash);
        }
        int totalCount = userDao.updateUser(user);
        //修改用户权限组信息
        if(this.checkUserByRole(user.getUserId())){
            UserRole role=new UserRole();
            role.setGroupId(user.getGroupId());
            role.setUserId(user.getUserId());
            userDao.updateUserAndGroup(role);
        }else{
            userDao.addUserAndGroup(user.getGroupId(),user.getUserId());
        }
        if(totalCount > 0){
            data = DataVo.jump("用户信息修改","/system/user/user_edit/"+user.getUserId());
        }else{
            data=DataVo.failure("更新失败，请联系管理员！");
        }
        return data;
    }

    /**
     * 修改用户头像
     *
     * @param userId
     *         用户id
     * @param avatar
     *         用户头像地址
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateAvatar(Long userId,String avatar) {
        return userDao.updateAvatar(userId,avatar);
    }

    /**
     * 更新统计用户所有提的问题数量
     *         问题数量
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateQuestionCount(Long userId) {
        return userDao.updateQuestionCount(userId);
    }

    /**
     * 更新统计用户所有关注的问题数量
     *         问题数量
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateQuestionFollowCount(Long userId) {
        return userDao.updateQuestionFollowCount(userId);
    }

    /**
     * 更新统计用户所有发布文章数量
     *
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateArticleCount(Long userId) {
        return userDao.updateArticleCount(userId);
    }

    /**
     * 更新统计用户所有发布文章数量
     *
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateShareCount(Long userId) {
        return userDao.updateShareCount(userId);
    }

    /**
     * 更新统计用户所有加入的话题数量
     *
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateTopicCount(Long userId) {
        return userDao.updateTopicCount(userId);
    }

    /**
     * 更新统计用户所有发布的答案数量
     *         问题数量
     * @param userId
     *         用户id
     * @return
     */
    @CacheEvict(value = "user", allEntries = true)
    public int updateAnswerCount(Long userId) {
        return userDao.updateAnswerCount(userId);
    }

    /**
     * 用户修改密码
     *
     * @param userId
     *        用户id
     * @param oldPassword
     *        旧密码
     * @param password
     *        新密码
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updatePassword(Long userId, String oldPassword, String password) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        User user = userDao.findUserById(userId,0);
        if (user == null) {
            return data = DataVo.failure("用户信息错误");
        }
        if (BCryptUtils.checkpw(oldPassword, user.getPassword())) {
            String pwHash = BCryptUtils.hashpw(password, BCryptUtils.gensalt());
            userDao.updatePassword(userId,pwHash);
        } else {
            return data = DataVo.failure("原始密码错误");
        }
        return data = DataVo.jump("密码已修改","/ucenter/password");
    }

    //前台用户信息修改信息
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updateUserAccount(User user){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkUserByNickName(user.getNickName(),user.getUserId())){
            return data = DataVo.failure("用户名已被占用！");
        }

        int totalCount = userDao.updateUser(user);
        if(totalCount > 0){
            if (this.checkUserByActivation(user.getUserId(),Long.parseLong(configService.getStringByKey("user_activation_role")))){
                //用户信息完善后修改权限组为正常用户
                UserRole userRole=new UserRole();
                userRole.setUserId(user.getUserId());
                userRole.setGroupId(Long.parseLong(configService.getStringByKey("user_role")));
                userDao.updateUserAndGroup(userRole);
            }
            data = DataVo.success("账户基本信息修改成功");
        }else{
            data=DataVo.failure("新用户添加失败！");
        }
        return data;
    }

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
    public int updateUserAccountScore(String calculate, Integer score, Long userId){
        return userDao.updateUserAccountScore(calculate,score,userId);
    }

    /**
     * 手机申请获取验证码
     *
     * @param phoneNumber
     *        手机号码
     * @return
     * @throws Exception
     */
    @Transactional
    public DataVo safeMobileCode(Long userId,String phoneNumber) throws Exception {
        if(this.checkUserByMobile(phoneNumber,userId)) {
            return DataVo.failure("该手机号码以注册或被占用！");
        }
        int count=userDao.checkUserActivationCount(phoneNumber,DateUtils.getDay());
        if(count<=5) {
            smsapiService.mobileRegisterCode(phoneNumber, MathUtils.getRandomCode(6),2);
        }else {
            return DataVo.failure("您今日已超出申请次数！");
        }
        return DataVo.success("验证码已发送", DataVo.NOOP);
    }

    /**
     * 用户安全手机号码，同时可登录使用
     *
     * @param userId
     *        用户id
     * @param mobile
     *        旧密码
     * @param code
     *        新密码
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updateSafeMobile(Long userId, String password, String mobile, String code) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        User user = userDao.findUserById(userId,0);
        if (user == null) {
            return data = DataVo.failure("用户信息错误");
        }
        if (this.checkUserActivationCode(mobile,2,code)) {
            //验证通过后修改本条记录为已验证
            userDao.updateUserActivationByStatus(mobile,code);
        }else {
            return DataVo.failure("手机验证码错误");
        }
        //查询手机号码是否被占用
        if(this.checkUserByMobile(mobile,userId)){
            return data =DataVo.failure("已被手机号码或者用户名占用！");
        }
        if (BCryptUtils.checkpw(password, user.getPassword())) {
            //修改登录手机号码
            userDao.updateuUserMobile(mobile,userId);
        } else {
            return data = DataVo.failure("当前登录密码错误");
        }
        return data = DataVo.jump("登录手机号码已更新","/ucenter/safe_mobile");
    }

    /**
     * 用户安全手机号码，同时可登录使用
     *
     * @param userId
     *        用户id
     * @param password
     *        登录密码
     * @param userEmail
     *        新修改邮箱
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updateSafeEmail(Long userId, String password, String userEmail, String code) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        User user = userDao.findUserById(userId,0);
        if (user == null) {
            return data = DataVo.failure("用户信息错误");
        }
        if (this.checkUserActivationCode(userEmail,2,code)) {
            //验证通过后修改本条记录为已验证
            userDao.updateUserActivationByStatus(userEmail,code);
        }else {
            return DataVo.failure("邮箱验证码错误");
        }
        //查询邮箱是否被占用
        if(this.checkUserByMobile(userEmail,userId)){
            return data =DataVo.failure("邮箱已用户名占用！");
        }
        if (BCryptUtils.checkpw(password, user.getPassword())) {
            //修改登录手机号码
            userDao.updateuUserEmail(userEmail,userId);
        } else {
            return data = DataVo.failure("当前登录密码错误");
        }
        return data = DataVo.jump("登录邮箱已更新","/ucenter/safe_email");
    }

    /**
     * 给指定用户邮箱发送绑定邮箱验证码邮件
     *
     * @param userEmail
     *        需要发送的用户信息
     * @return
     * @throws Exception
     */
    @Transactional
    public DataVo safeEmailVerify(String userEmail,Long userId) throws Exception {
        if (this.checkUserByEmail(userEmail,userId)) {
            return DataVo.failure("该用户已存在，请换其他账户！");
        }
        String code=CaptchaUtils.getUserCaptcha().toLowerCase();
        UserActivation activation=new UserActivation();
        activation.setUserName(userEmail);
        activation.setCode(code);
        activation.setCodeType(2);
        activation.setReferStatus(0);
        activation.setReferTime(new Date());
        userDao.addUserActivation(activation);
        emailService.sendEmail(userEmail,code, "safe_email");
        return DataVo.success("验证码已发送，请去邮箱查收！", DataVo.NOOP);
    }

    /**
     * 用户修改密码
     *
     * @param username
     *        用户id
     * @param code
     *        旧密码
     * @param password
     *        新密码
     * @throws Exception
     */
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public DataVo updateGetBackPassword(String username, String code, String password) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        User user = null;
        if(StringHelperUtils.checkPhoneNumber(username)) {
            user = userDao.findByMobile(username);
        }else if(StringHelperUtils.emailFormat(username)){
            user = userDao.findByEmail(username);
        }
        if (user == null) {
            return data = DataVo.failure("用户账号不存在！");
        }
        if (this.checkUserActivationCode(username,3,code)) {
            String pwHash = BCryptUtils.hashpw(password, BCryptUtils.gensalt());
            userDao.updatePassword(user.getUserId(),pwHash);
            //验证通过后修改本条记录为已验证
            userDao.updateUserActivationByStatus(username,code);
        } else {
            return data = DataVo.failure("验证码错误");
        }
        return data = DataVo.jump("密码已修改，请重新登录","/login");
    }

    /**
     * 给指定用户邮箱发送重置密码邮件
     *
     * @param userEmail
     *        需要发送的用户信息
     * @return
     * @throws Exception
     */
    @Transactional
    public DataVo getEmailBackCode(String userEmail) throws Exception {
        if (!this.checkUserByEmail(userEmail,null)) {
            return DataVo.failure("该用户不存在，请检查账户是否正确！");
        }
        String code=CaptchaUtils.getUserCaptcha().toLowerCase();
        UserActivation activation=new UserActivation();
        activation.setUserName(userEmail);
        activation.setCode(code);
        activation.setCodeType(3);
        activation.setReferStatus(0);
        activation.setReferTime(new Date());
        userDao.addUserActivation(activation);
        emailService.sendEmail(userEmail,code, "reset_email");
        //验证通过后修改本条记录为已验证
        userDao.updateUserActivationByStatus(userEmail,code);
        return DataVo.success("验证码已发送，请去邮箱查收！", DataVo.NOOP);
    }

    //添加用户保持登录状态记录
    public int updateUserSession(UserSession session) {
        return userDao.updateUserSession(session);
    }
    // ///////////////////////////////
    // /////        查询      ////////
    // ///////////////////////////////
    @Cacheable(value = "user",key="#shortUrl")
    public User findUserByShorturl(String shortUrl){
        return userDao.findUserByShorturl(shortUrl);
    }

    /**
     * 通过userId查询用户信息
     * 用户状态0是所有 1未审核 2正常状态 3 删除至回收站 4锁定
     *
     * @param userId
     *         用户id
     * @param  status
     *          审核状态
     * @return User
     */
    @Cacheable(value = "user")
    public User findUserById(Long userId,Integer status) {
        return userDao.findUserById(userId,status);
    }

    /**
     * 按用户id查询用户统计信息
     *
     * @param userId
     *         用户id
     * @return UserAccount
     */
    @Cacheable(value = "user")
    public UserCount findUserCountById(Long userId) {
        return userDao.findUserCountById(userId);
    }

    /**
     * 按用户id查询用户信息
     *
     * @param userId
     *         用户id
     * @return UserAccount
     */
    @Cacheable(value = "user")
    public UserAccount findUserAccountById(Long userId) {
        return userDao.findUserAccountById(userId);
    }

    //按id查询用户是否存在
    public boolean checkUserById(Long userId){
        int totalCount = userDao.checkUserById(userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询话题短域名是否被占用
     *
     * @param shortUrl
     * @return
     */
    public boolean checkUserByShorturl(String shortUrl) {
        int totalCount = userDao.checkUserByShorturl(shortUrl);
        return totalCount > 0 ? true : false;
    }

    public String shortUrl(){
        String[] aResult = ShortUrlUtils.shortUrl (null);
        String code=null;
        for ( int i = 0; i < aResult. length ; i++) {
            code=aResult[i];
            //查询文章短域名是否被占用
            if(!this.checkUserByShorturl(code)){
                break;
            }
        }
        return code;
    }

    /**
     * 通过username查询用户信息
     *
     * @param userName
     * @return User
     */
    @Cacheable(value = "user")
    public User findByUsername(String userName) {
        return userDao.findByUsername(userName);
    }

    /**
     * 查询当前用户名是否被占用
     *
     * @param userName
     *        用户名
     * @return
     */
    public boolean checkUserByUsername(String userName) {
        return userDao.findByUsername(userName) != null;
    }

    /**
     * 查询当前用户名是否被占用
     *
     * @param userName
     *        用户名
     * @return
     */
    public boolean findByMobile(String userName) {
        return userDao.findByMobile(userName) == null;
    }

    /**
     * 排除当前用户id后查询用户名是否存在,如果userId设置为null则查全部的用户名
     *
     * @param userName
     *         用户登录手机号码
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public boolean checkUserByUserName(String userName,Long userId) {
        int totalCount = userDao.checkUserByUserName(userName,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 排除当前用户id后查询手机号码是否存在,如果userId设置为null则查全部的手机号码
     *
     * @param userMobile
     *         用户登录手机号码
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public boolean checkUserByMobile(String userMobile,Long userId) {
        int totalCount = userDao.checkUserByMobile(userMobile,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 排除当前用户id后查询当前邮箱是否存在,如果userId设置为null则查全部的邮箱
     *
     * @param userEmail
     *         用户登录邮箱地址
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public boolean checkUserByEmail(String userEmail,Long userId) {
        int totalCount = userDao.checkUserByEmail(userEmail,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 排除当前用户id后查询当前昵称是否存在,如果userId设置为null则查全部的昵称
     *
     * @param nickName
     *         用户登录邮箱地址
     * @param userId
     *         需要排除的user_id,可设置为null
     * @return
     */
    public boolean checkUserByNickName(String nickName,Long userId) {
        int totalCount = userDao.checkUserByNickName(nickName,userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询当前用户权限是否存在
     *
     * @param userId
     *         用户id
     * @return
     */
    public boolean checkUserByRole(Long userId) {
        int totalCount = userDao.checkUserByRole(userId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询当前用户是否是未激活权限组内用户
     *
     * @param userId
     *         用户id
     * @param groupId
     *         未激活权限组id
     * @return
     */
    public boolean checkUserByActivation(Long userId,Long groupId) {
        int totalCount = userDao.checkUserByActivation(userId,groupId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询验证码在当前时间5分钟内获取并且是否过时或不存在
     *
     * @param userName
     *         查询的用户名
     * @param codeType
     *         查询的验证码类型，1手机注册验证码,2安全手机设置验证码,3密码重置验证码
     * @param code
     *         验证码
     * @return
     */
    public boolean checkUserActivationCode(String userName,Integer codeType,String code){
        UserActivation activation = userDao.findByUserActivationCode(userName,codeType);
        if(activation!=null){
            if(activation.getCode().equals(code)){
                return true;
            }
        }
        return false;
    }

    /**
     * 查询是否已关注或者是该用户粉丝
     *不存在返回：true
     *
     * @param userFollow
     *         关注者id
     * @param userFans
     *         粉丝id
     * @return
     */
    @Cacheable(value = "user")
    public boolean checkUserFans(Long userFollow,Long userFans){
        int totalCount = userDao.checkUserFans(userFollow,userFans);
        return totalCount > 0 ? true : false;
    }

    /**
     * 查询是否已关注或者是该用户粉丝
     *不存在返回：true
     *
     * @param userFollow
     *         被关注者id
     * @param userFans
     *         粉丝id
     * @return
     */
    @Cacheable(value = "user")
    public boolean checkUserMutualFans(Long userFollow,Long userFans){
        int totalCount = userDao.checkUserMutualFans(userFollow,userFans);
        return totalCount == 2 ? true : false;
    }

    /**
     * 昵称检查是否已保护
     *
     * @param nickname
     * @return
     */
    public boolean kbbeNickname(String nickname){
        String kbbeUser=configService.getStringByKey("user_notallow");
        if(kbbeUser!=null){
            String[] notallow = kbbeUser.split(","); //转换为数组
            for (String s : notallow) {
                if (s.equals(nickname)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * 用户翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<User> getUserListPage(String username,String trueName,String mobile,String email,String orderby,String order,int pageNum, int rows) {
        PageVo<User> pageVo = new PageVo<User>(pageNum);
        pageVo.setRows(rows);
        List<User> list = new ArrayList<User>();
        if(orderby==null){
            orderby="user_id";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(userDao.getUserList(username, trueName, mobile, email,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userDao.getUserCount(username, trueName, mobile, email));
        return pageVo;
    }


    /**
     * 用户粉丝翻页查询
     *
     * @param userFollow
     *        被关注者用户id
     * @param userFans
     *         关注者的id
     * @param createTime
     *         关注时间
     * @param orderby
     *         排序类型：id、time
     * @param order
     *         排序方式，desc、asc
     * @param pageNum
     *         当前页数
     * @param rows
     *         每页数量
     * @return
     */
    public PageVo<UserFans> getUserFansListPage(Long userFollow,Long userFans,String createTime,String orderby,String order,int pageNum, int rows) {
        PageVo<UserFans> pageVo = new PageVo<UserFans>(pageNum);
        pageVo.setRows(rows);
        List<UserFans> list = new ArrayList<UserFans>();
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(userDao.getUserFansList(userFollow, userFans, createTime,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userDao.getUserFansCount(userFollow, userFans, createTime));
        return pageVo;
    }

    /**
     * 热门用户翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<User> getUserHotListPage(String userName,String nickName,String mobile,String email,Integer province,Integer city,Integer area,Integer status,String orderby,String order,int pageNum, int rows) {
        PageVo<User> pageVo = new PageVo<User>(pageNum);
        pageVo.setRows(rows);
        List<User> list = new ArrayList<User>();
        if(orderby==null){
            orderby="a.score";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(userDao.getUserHotList(userName, nickName, mobile, email,province,city,area,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userDao.getUserHotCount(userName, nickName, mobile, email,province,city,area,status));
        return pageVo;
    }

    /**
     * 根据UserId查询用户登录时间
     *
     * @param sessionKey
     *        用户seeeionKey
     * @return
     */
    public UserSession findUserSessionBySeeeionKey(String sessionKey) {
        return userDao.findUserSessionBySeeeionKey(sessionKey);
    }

    /**
     * 登录会话是否已过期
     */
    public boolean isExpireTime(Long expireTime) {
        //如果小于当前时间加120分钟说明不是用户选择的长期在线
        return expireTime < System.currentTimeMillis()+(120 * 60 * 1000);
    }

    /**
     * 查询当前用户登陆状态是否存在
     *
     * @param userId
     *         当前用户id
     * @return
     */
    public boolean checkUserSessionByUserId(Long userId) {
        int totalCount = userDao.checkUserSessionByUserId(userId);
        return totalCount > 0 ? true : false;
    }
}