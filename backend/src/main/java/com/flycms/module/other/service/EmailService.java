/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */
package com.flycms.module.other.service;

import java.util.*;

import jakarta.mail.Authenticator;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import com.flycms.core.utils.DateUtils;
import com.flycms.core.utils.PlaceholderUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.other.dao.EmailDao;
import com.flycms.module.other.model.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 邮件服务（第三方 SMTP）
 *
 * SMTP 参数全部来自网站设置（fly_config_web）：
 *   fly_smtp_server    SMTP 服务器地址，如 smtp.qq.com
 *   fly_smtp_port      端口：SSL 465 / STARTTLS 587 / 不加密 25
 *   fly_smtp_ssl       加密方式：1 SSL(默认) / 2 STARTTLS / 0 不加密
 *   fly_smtp_usermail  发件邮箱账号
 *   fly_smtp_password  授权码（QQ/163 等第三方邮箱用授权码而非登录密码）
 *   fly_smtp_fromname  发件人昵称（可选）
 *
 * 业务调用方：UserService（safe_email 邮箱绑定验证码 / reset_email 找回密码）、
 * FormService（表单提交通知）、本服务 sendTestEmail（网站设置里的连通性测试）。
 *
 * @author lwq
 */
@Service
public class EmailService {
	private static final org.slf4j.Logger log =
			org.slf4j.LoggerFactory.getLogger(EmailService.class);

	@Autowired
	protected ConfigService configService;
    @Autowired
    protected EmailDao emailDao;

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////

    /**
     * 给指定用户邮箱发送模板邮件（验证码类）
     *
     * @param userEmail 收件邮箱
     * @param code      验证码
     * @param tpCode    后台设置的邮件模板key（safe_email / reset_email）
     */
    public void sendEmail(String userEmail, String code, String tpCode) throws MessagingException {
        Email email = emailDao.findEmailTempletByTpCode(tpCode);
        Map<String, String> map = new HashMap<String, String>();
        map.put("code", code);
        map.put("userEmail", userEmail);
        map.put("createTime", DateUtils.getTime());
        String mailBody = PlaceholderUtils.resolvePlaceholders(email.getContent(), map);
        String subject = email.getTitle() == null ? "系统邮件" : email.getTitle();
        doSend(userEmail, subject, mailBody);
    }

    /**
     * 发送通用通知邮件（表单提交通知等）
     *
     * @param toEmail 收件人
     * @param subject 邮件标题
     * @param content 正文（HTML 片段）
     * @return 是否发送成功
     */
    public boolean sendNotifyEmail(String toEmail, String subject, String content) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }
        try {
            doSend(toEmail, subject == null ? "系统通知" : subject, content);
            return true;
        } catch (Exception e) {
            log.error("发送通知邮件失败, toEmail={}", toEmail, e);
            return false;
        }
    }

    /**
     * 网站设置里的「发送测试邮件」：用当前保存的 SMTP 配置直发一封测试邮件。
     *
     * @param toEmail 测试收件地址
     * @return null 表示成功；否则返回人类可读的错误信息
     */
    public String sendTestEmail(String toEmail) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return "请填写测试收件邮箱";
        }
        String host = configService.getStringByKey("fly_smtp_server");
        if (host == null || host.trim().isEmpty()) {
            return "请先保存 SMTP 服务器地址";
        }
        try {
            doSend(toEmail.trim(),
                    "FlyCms SMTP 配置测试邮件",
                    "这是一封测试邮件。收到即说明当前网站设置的第三方邮箱参数可用。<br/>"
                            + "发送时间：" + DateUtils.getTime());
            return null;
        } catch (Exception e) {
            log.error("SMTP 测试邮件发送失败, toEmail={}", toEmail, e);
            String msg = e.getMessage();
            return msg == null ? e.getClass().getName() : msg;
        }
    }

    // ///////////////////////////////
    // /////       内部        ////////
    // ///////////////////////////////

    /** 从网站设置读取 fly_smtp_* 组装会话并发送（统一 UTF-8） */
    private void doSend(String toEmail, String subject, String htmlBody) throws MessagingException {
        Properties props = buildSmtpProps();
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        props.getProperty("mail.user"), props.getProperty("mail.password"));
            }
        };
        Session mailSession = Session.getInstance(props, authenticator);
        MimeMessage message = new MimeMessage(mailSession);
        // 发件人（带昵称；昵称缺失时直接用邮箱地址）
        String fromName = configService.getStringByKey("fly_smtp_fromname");
        String fromAddr = props.getProperty("mail.user");
        try {
            if (fromName != null && !fromName.trim().isEmpty()) {
                message.setFrom(new InternetAddress(fromAddr, fromName.trim(), "UTF-8"));
            } else {
                message.setFrom(new InternetAddress(fromAddr));
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw new MessagingException("发件人昵称编码错误", e);
        }
        message.setRecipient(RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject, "UTF-8");
        message.setText("<html><head><meta charset='utf-8'></head><body>" + htmlBody
                + "</body></html>", "UTF-8", "html");
        message.setSentDate(new Date());
        Transport.send(message);
    }

    /** 加密方式：1 SSL(默认) / 2 STARTTLS / 0 不加密 */
    private Properties buildSmtpProps() {
        Properties props = new Properties();
        String host = configService.getStringByKey("fly_smtp_server");
        String user = configService.getStringByKey("fly_smtp_usermail");
        String password = configService.getStringByKey("fly_smtp_password");
        String port = configService.getStringByKey("fly_smtp_port");
        int sslMode = 1;
        try {
            sslMode = Integer.parseInt(configService.getStringByKey("fly_smtp_ssl"));
        } catch (Exception ignore) {
            // 未配置或非数字按默认 SSL 处理
        }
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host == null ? "" : host.trim());
        props.put("mail.user", user == null ? "" : user.trim());
        props.put("mail.password", password == null ? "" : password.trim());
        props.put("mail.smtp.port", port == null || port.trim().isEmpty() ? "25" : port.trim());
        if (sslMode == 2) {
            // STARTTLS：587 端口常见
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.socketFactory.fallback", "true");
        } else if (sslMode == 0) {
            // 不加密：25 端口常见
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            // SSL：465 端口常见
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", props.getProperty("mail.smtp.port"));
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        return props;
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改邮件模板
    public DataVo updateEmailTempletsById(Email email){
        DataVo data = DataVo.failure("操作失败");
        if(email.getId()==null){
            return DataVo.failure("传递参数错误！");
        }
        if(email.getTitle()==null){
            return DataVo.failure("邮件模板标题不能为空！");
        }
        if(email.getContent()==null){
            return DataVo.failure("邮件模板内容不能为空！");
        }
        int total = emailDao.updateEmailTempletsById(email);
        if(total>0){
            data=DataVo.jump("用户更新成功！","/admin/email/list_email");
        }else{
            data=DataVo.failure("添加失败");
        }
        return data;
    }


    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    public Email findEmailTempletById(Integer id){
        return emailDao.findEmailTempletById(id);
    }
    /**
     * 查看邮件模板列表分页
     *
     * @return PageVo<Email>
     */
    public PageVo<Email> getEmailTempletPage(int pageNum, int rows){
        PageVo<Email> pageVo = new PageVo<Email>(pageNum);
        pageVo.setRows(rows);
        List<Email> list = new ArrayList<Email>();
        int count = emailDao.getEmailTempletCount();
        pageVo.setList(emailDao.getEmailTempletList(pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(count);
        return pageVo;
    }
}
