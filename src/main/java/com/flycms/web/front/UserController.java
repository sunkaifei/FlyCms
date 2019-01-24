package com.flycms.web.front;
import com.flycms.core.utils.Base64HelperUtils;
import com.flycms.core.utils.CookieUtils;
import com.flycms.core.utils.DateUtils;
import com.flycms.core.utils.StringHelperUtils;
import com.flycms.module.user.utils.UserSessionUtils;
import com.flycms.constant.Const;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.question.service.ImagesService;
import com.flycms.module.user.model.User;
import com.flycms.module.user.service.UserService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;

@Controller
public class UserController extends BaseController {
    @Autowired
    protected UserService userService;

    @Autowired
    private ImagesService imagesService;

    //用户注册
    @GetMapping(value = "/reg")
    public String userReg(@RequestParam(value = "invite", required = false) String invite,ModelMap modelMap){
        if(invite==null){
            invite=CookieUtils.getCookie(request,"invite");
        }
        modelMap.addAttribute("invite",invite);
        return theme.getPcTemplate("user/reg");
    }

    /**
     * 用户提交手机号码申请获取验证码
     *
     * @param username
     *        提交的手机号码
     * @param captcha
     *        验证码
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping(value = "/ucenter/mobilecode")
    public DataVo getAddUserMobileCode(@RequestParam(value = "username", required = false) String username,@RequestParam(value = "captcha", required = false) String captcha) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        String kaptcha = (String) session.getAttribute("kaptcha");
        // 校验验证码
        if (captcha != null) {
            if (!captcha.equalsIgnoreCase(kaptcha)) {
                return DataVo.failure("验证码错误");
            }
            session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        }else{
            return DataVo.failure("验证码不能为空");
        }
        if(!StringHelperUtils.checkPhoneNumber(username)) {
            return DataVo.failure("手机号码错误！");
        }
        data = userService.regMobileCode(username);
        return data;
    }

    /**
     * 添加新用户
     *
     * @param username
     * @param password
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/ucenter/reg_user")
    public DataVo addUser(@RequestParam(value = "username", required = false) String username,
                          @RequestParam(value = "password", required = false) String password,
                          @RequestParam(value = "password2", required = false) String password2,
                          @RequestParam(value = "invite", required = false) String invite,
                          @RequestParam(value = "captcha", required = false) String captcha
    ) {
        DataVo data = DataVo.failure("操作失败");
        try {
            username=username.trim();
            password=password.trim();
            password2=password2.trim();
            captcha=captcha.trim();
            String kaptcha = (String) session.getAttribute(Const.KAPTCHA_SESSION_KEY);
            // 校验验证码
            if (captcha == null && "".equals(captcha)) {
                return DataVo.failure("验证码不能为空");
            }
            captcha=captcha.toLowerCase();
            if(!captcha.equals(kaptcha)){
                return DataVo.failure("验证码错误");
            }

            if (StringUtils.isBlank(username)) {
                return DataVo.failure("用户名不能为空");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("密码不能为空");
            }
            if (password.length() < 6) {
                return DataVo.failure("密码不能小于6位");
            }
            if (password.length() > 16) {
                return DataVo.failure("密码不能大于16位");
            }
            if (!password.equals(password2)) {
                return DataVo.failure("密码两次输入不一致");
            }
            data = userService.addUserReg(3,username, password,null,invite,request,response);
            return DataVo.success("操作成功");
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/addMobileUser")
    public DataVo addMobileUser(@RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                @RequestParam(value = "mobilecode", required = false) String mobilecode,
                                @RequestParam(value = "password", required = false) String password,
                                @RequestParam(value = "password2", required = false) String password2,
                                @RequestParam(value = "invite", required = false) String invite) throws Exception{
        DataVo data = DataVo.failure("操作失败");
        if (mobilecode == null) {
            return data = DataVo.failure("验证码不能为空");
        }
        if (password == null) {
            return data = DataVo.failure("密码不能为空");
        }
        if(!password.equals(password2)){
            return data = DataVo.failure("两次密码输入不一样");
        }
        return data = userService.addUserReg(1,phoneNumber, password,mobilecode,invite,request,response);
    }

    //用户登录页面
    @GetMapping(value = "/login")
    public String userLogin(@RequestParam(value = "redirectUrl",required = false) String redirectUrl,ModelMap modelMap){
        if(getUser() != null){
            return "redirect:/index";
        }
        modelMap.addAttribute("redirectUrl",redirectUrl);
        return theme.getPcTemplate("user/login");
    }

    //登录处理
    @ResponseBody
    @PostMapping(value = "/ucenter/login_act")
    public DataVo userLogin(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "rememberMe", required = false) String rememberMe,
            @RequestParam(value = "redirectUrl",required = false) String redirectUrl,
            @RequestParam(value = "captcha", required = false) String captcha) {
        try {
            String kaptcha = (String) session.getAttribute("kaptcha");
            if (StringUtils.isBlank(username)) {
                return DataVo.failure("用户名不能为空");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("密码不能为空");
            } else if (password.length() < 6 && password.length() > 30) {
                return DataVo.failure("密码最少6个字符，最多30个字符");
            }
            // 校验验证码
            if (captcha != null) {
                if (!captcha.equalsIgnoreCase(kaptcha)) {
                    return DataVo.failure("验证码错误");
                }
            }else{
                return DataVo.failure("验证码不能为空");
            }
            boolean keepLogin = "1".equals(rememberMe) ? true : false;
            User entity = userService.userLogin(username,password,keepLogin,request,response);
            if(entity==null){
                return DataVo.failure("帐号或密码错误。");
            }else{
                session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
                if (StringUtils.isNotEmpty(redirectUrl)){
                    return DataVo.jump("操作成功", redirectUrl);
                }
                return DataVo.jump("操作成功", "/");
            }
        } catch (Exception e) {
            return DataVo.failure("帐号或密码错误。");
        }
    }

    /**
     * 页面ajax登录处理
     *
     * @param username
     * @param password
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/ucenter/ajaxlogin")
    public DataVo userAjaxLogin( @RequestParam(value = "username", required = false) String username,
                                 @RequestParam(value = "password", required = false) String password,
                                 @RequestParam(value = "rememberMe", required = false) String rememberMe,
                                 @RequestParam(value = "code", required = false) String code) {
        DataVo data = DataVo.failure("操作失败");
        try {
            String kaptcha = (String) session.getAttribute("kaptcha");
            if (StringUtils.isBlank(username)) {
                return DataVo.failure("用户名不能为空");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("密码不能为空");
            } else if (password.length() < 6 && password.length() > 30) {
                return DataVo.failure("密码最少6个字符，最多30个字符");
            }
            // 校验验证码
            if (code != null) {
                if (!code.equalsIgnoreCase(kaptcha)) {
                    return DataVo.failure("验证码错误");
                }
            }else{
                return DataVo.failure("验证码不能为空");
            }
            boolean keepLogin = "1".equals(rememberMe) ? true : false;
            User entity = userService.userLogin(username,password,keepLogin,request,response);
            if(entity==null){
                return DataVo.failure("帐号或密码错误。");
            }else{
                session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
                return DataVo.jump("操作成功", "/");
            }
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //修改用户基本信息
    @GetMapping(value = "/ucenter/account")
    public String userAccount(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/account");
    }

    //更新用户基本信息
    @PostMapping("/ucenter/account_update")
    @ResponseBody
    public DataVo updateUserAccount(@Valid User user, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            if(!StringUtils.isNumeric(user.getUserId().toString())){
                return data=DataVo.failure("请勿非法传递数据！");
            }
            if(!user.getUserId().equals(getUser().getUserId())){
                return data=DataVo.failure("请勿非法传递数据！");
            }
            if(!getUser().getUserId().equals(user.getUserId())){
                return data=DataVo.failure("只能修改属于自己的基本信息！");
            }
            if(StringUtils.isBlank(user.getNickName())){
                return data=DataVo.failure("昵称不能为空！");
            }
            if(user.getBirthday()==null || "".equals(user.getBirthday())){
                return data=DataVo.failure("请选择您的生日日期！");
            }
            if(DateUtils.isValidDate(user.getBirthday().toString())){
                return data=DataVo.failure("生日日期格式错误！");
            }
            if(user.getProvince()==0){
                return data=DataVo.failure("省份未选择！");
            }
            if(user.getCity()==0){
                return data=DataVo.failure("地区为选择！");
            }

            data = userService.updateUserAccount(user);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //安全手机账号设置
    @GetMapping(value = "/ucenter/safe_mobile")
    public String safeMobile(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/safe_mobile");
    }

    /**
     * 用户提交手机号码申请获取验证码
     *
     * @param mobile
     *        提交的手机号码
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping(value = "/ucenter/safemobilecode")
    public DataVo safeMobileCode(@RequestParam(value = "mobile", required = false) String mobile, @RequestParam(value = "captcha", required = false) String captcha) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        String kaptcha = (String) session.getAttribute("kaptcha");
        // 校验验证码
        if (captcha != null) {
            if (!captcha.equalsIgnoreCase(kaptcha)) {
                return DataVo.failure("验证码错误");
            }
            session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        }else{
            return DataVo.failure("验证码不能为空");
        }
        if(!StringHelperUtils.checkPhoneNumber(mobile)) {
            return DataVo.failure("手机号码错误！");
        }
        data = userService.safeMobileCode(getUser().getUserId(),mobile);
        return data;
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/safe_mobile_update")
    public DataVo safeMobile(
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "code", required = false) String code) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("密码不能为空");
            } else if (password.length() < 6 && password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if(!StringHelperUtils.checkPhoneNumber(mobile)) {
                return DataVo.failure("手机号码错误！");
            }
            if (code == null && "".equals(code)) {
                return DataVo.failure("验证码不能为空");
            }
            data=userService.updateSafeMobile(getUser().getUserId(),password, mobile, code);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //设置安全邮箱账号
    @GetMapping(value = "/ucenter/safe_email")
    public String safeEmail(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/safe_email");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/safe_email_code")
    public DataVo userAjaxMailCaptcha(@RequestParam(value = "userEmail", required = false) String userEmail) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!StringHelperUtils.emailFormat(userEmail)) {
                return DataVo.failure("邮箱格式错误！");
            }
            return userService.safeEmailVerify(userEmail,getUser().getUserId());
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/safe_email_update")
    public DataVo safeEmail(
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "code", required = false) String code) {
        DataVo data = DataVo.failure("操作失败");
        try {
            // 校验验证码
            if (code == null && "".equals(code)) {
                return DataVo.failure("验证码不能为空");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("新密码不能为空");
            } else if (password.length() < 6 && password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if(!StringHelperUtils.emailFormat(userEmail)) {
                return DataVo.failure("邮箱地址错误！");
            }
            data=userService.updateSafeEmail(getUser().getUserId(), password, userEmail,code);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //我的积分
    @GetMapping(value = "/ucenter/integral")
    public String userIntegral(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/integral");
    }

    //我的退款申请
    @GetMapping(value = "/ucenter/refunds")
    public String userRefunds(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/refunds");
    }

    //我的网站建议
    @GetMapping(value = "/ucenter/complain")
    public String userComplain(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/complain");
    }

    //我的产品收藏
    @GetMapping(value = "/ucenter/favorite")
    public String userFavorite(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/favorite");
    }

    //线上推广列表
    @GetMapping(value = "/ucenter/invite")
    public String userInvite(@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        modelMap.addAttribute("p", p);
        return theme.getPcTemplate("user/invite");
    }

    //我的账户余额
    @GetMapping(value = "/ucenter/account_log")
    public String userAccount_log(@RequestParam(value = "p", defaultValue = "1") int p,ModelMap modelMap){
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/account_log");
    }

    //我的账户余额体现申请
    @GetMapping(value = "/ucenter/withdraw")
    public String userWithdraw(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/withdraw");
    }

    //我的在线充值
    @GetMapping(value = "/ucenter/online_recharge")
    public String userOnlineRecharge(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/online_recharge");
    }

    //我的收货地址管理
    @GetMapping(value = "/ucenter/address")
    public String userAddress(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/address");
    }

    //我的个人信息
    @GetMapping(value = "/ucenter/info")
    public String userInfo(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/info");
    }

    //我的密码修改
    @GetMapping(value = "/ucenter/password")
    public String userPassword(ModelMap modelMap){
        modelMap.addAttribute("user", getUser());
        return theme.getPcTemplate("user/password");
    }

    @ResponseBody
    @PostMapping(value = "/ucenter/password_update")
    public DataVo updatePassword(
            @RequestParam(value = "old_password", required = false) String old_password,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "password_confirmation", required = false) String password_confirmation,
            @RequestParam(value = "captcha", required = false) String captcha) {
        String kaptcha = (String) session.getAttribute("kaptcha");
        DataVo data = DataVo.failure("操作失败");
        // 校验验证码
        if (captcha == null && "".equals(captcha)) {
            return DataVo.failure("验证码不能为空");
        }
        captcha=captcha.toLowerCase();
        if(!captcha.equals(kaptcha)){
            return DataVo.failure("验证码错误");
        }
        try {
            if (StringUtils.isBlank(old_password)) {
                return DataVo.failure("原来密码不能为空");
            } else if (old_password.length() < 6 && old_password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("新密码不能为空");
            } else if (password.length() < 6 && password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if (!password.equals(password_confirmation)) {
                return DataVo.failure("两次密码必须一样");
            }
            data=userService.updatePassword(getUser().getUserId(), old_password, password);
            session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    /**
     * 保存头像
     *
     * @param avatar
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @ResponseBody
    @PostMapping("/ucenter/avatar.json")
    public DataVo changeAvatar(String avatar) throws IOException, ParseException {
        DataVo data = DataVo.failure("操作失败");
        if (StringUtils.isEmpty(avatar)) {
            return DataVo.failure("头像不能为空");
        }

        byte[] bytes;
        try {
            String _avatar = avatar.substring(avatar.indexOf(",") + 1, avatar.length());
            bytes = Base64HelperUtils.decode(_avatar);
        } catch (Exception e) {
            e.printStackTrace();
            return DataVo.failure("头像格式不正确");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage bufferedImage = ImageIO.read(bais);
        data =imagesService.saveAvatarDataFile(getUser(), bufferedImage);
        bais.close();
        return data;
    }

    //处理关注用户信息
    @ResponseBody
    @PostMapping(value = "/ucenter/user/follow")
    public DataVo userFollow(@RequestParam(value = "id", required = false) String id) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("问题参数错误");
            }
            if(getUser()==null){
                return data=DataVo.failure("请登陆后关注");
            }
            if((getUser().getUserId().equals(Long.parseLong(id)))){
                return data=DataVo.failure("无法关注自己！");
            }
            data=userService.addUserFans(Long.parseLong(id),getUser().getUserId());
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    /*
     *
     * 前台JS读取用户登录状态判断
     *
     */
    @ResponseBody
    @GetMapping(value = "/user/status.json")
    public void userSession() throws Exception {
        PrintWriter out =null;
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-type", "text/html;charset=utf-8");
        response.setContentType("text/javascript;charset=utf-8");
        /*response.setHeader("Cache-Control", "no-cache");*/
        out = response.getWriter();
        //out.flush();//清空缓存
        if(getUser()!=null){
            out.println("var userid='"+getUser().getUserId()+"';");
            out.println("var nickname = '"+getUser().getNickName()+"';");
            String signature="";
            if(getUser().getSignature()!=null){
                signature=getUser().getSignature();
            }else{
                signature="这个家伙很懒，啥也没留下！";
            }
            out.println("var signature = '"+signature+"';");
            String avatar=getUser().getAvatar();
            if(avatar==null){
                avatar="/assets/skin/pc_theme/defalut/images/avatar/default.jpg";
            }
            out.println("var smallAvatar = '"+avatar+"';");
        }else{
            out.println("var userid='';");
            out.println("var nickname = '';");
            out.println("var signature = '';");
            out.println("var smallAvatar = '/assets/skin/pc_theme/defalut/images/avatar/default.jpg';");
        }
        out.close();
    }

    /**
     * 用户退出登录
     *
     */
    @GetMapping(value = "/logout")
    public String logout() {
        //清除cookie、session
        userService.signOutLogin(request,response);
        return "redirect:/index-hot";
    }

    /**
     * 用户选择找回密码方式
     *
     */
    @GetMapping(value = "/forget_password")
    public String forgetPassword(ModelMap modelMap) {
        if(getUser()!=null){
            modelMap.addAttribute("user", getUser());
        }
        return theme.getPcTemplate("user/forget_password");
    }

    /**
     * 用户选择找回密码方式
     *
     */
    @GetMapping(value = "/forget_password/mobile")
    public String forgetPasswordMobile(ModelMap modelMap) {
        if(getUser()!=null){
            modelMap.addAttribute("user", getUser());
        }
        return theme.getPcTemplate("user/forget_password_mobile");
    }

    /**
     * 用户提交手机号码申请获取验证码
     *
     * @param username
     *        提交的手机号码
     * @param captcha
     *        验证码
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping(value = "/forget_password/getbackcode")
    public DataVo getBackCode(@RequestParam(value = "username", required = false) String username,@RequestParam(value = "captcha", required = false) String captcha) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        String kaptcha = (String) session.getAttribute("kaptcha");
        // 校验验证码
        if (captcha != null) {
            if (!captcha.equalsIgnoreCase(kaptcha)) {
                return DataVo.failure("验证码错误");
            }
            session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        }else{
            return DataVo.failure("验证码不能为空");
        }
        if (StringUtils.isBlank(username)) {
            return DataVo.failure("手机号码不能为空");
        }
        if(!StringHelperUtils.checkPhoneNumber(username)) {
            return DataVo.failure("手机号码错误！");
        }
        data = userService.userGetBackCode(username);
        return data;
    }

    /**
     * 用户选择找回密码方式
     *
     */
    @GetMapping(value = "/forget_password/email")
    public String forgetPasswordEmail(ModelMap modelMap) {
        if(getUser()!=null){
            modelMap.addAttribute("user", getUser());
        }
        return theme.getPcTemplate("user/forget_password_email");
    }

    /**
     * 用户提交邮箱地址申请获取验证码
     *
     * @param username
     *        提交的手机号码
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping(value = "/forget_password/getbackemailcode")
    public DataVo getEmailBackCode(@RequestParam(value = "username", required = false) String username) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        if (StringUtils.isBlank(username)) {
            return DataVo.failure("手机号码不能为空");
        }
        if (!StringHelperUtils.emailFormat(username)) {
            return DataVo.failure("邮箱格式错误！");
        }
        return data = userService.getEmailBackCode(username);
    }

    /**
     * 用户提交手机号码申请获取验证码
     *
     * @param username
     *        用户邮箱或者手机号码
     * @param code
     *        验证码
     * @param password
     *        重新设置的新密码
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping(value = "/forget_password/update_password")
    public DataVo updateUserPassword(@RequestParam(value = "username", required = false) String username,
                                     @RequestParam(value = "code", required = false) String code,
                                     @RequestParam(value = "password", required = false) String password) throws Exception {
        DataVo data = DataVo.failure("操作失败");
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (StringUtils.isBlank(username)) {
            return DataVo.failure("用户名不能为空");
        }
        if (StringUtils.isBlank(code)){
            return DataVo.failure("验证码不能为空");
        }
        if (StringUtils.isBlank(password)) {
            return DataVo.failure("密码不能为空");
        } else if (password.length() < 6 && password.length() > 30) {
            return DataVo.failure("密码最少6个字符，最多30个字符");
        }
        data = userService.updateGetBackPassword(username,code,password);
        return data;
    }
}
