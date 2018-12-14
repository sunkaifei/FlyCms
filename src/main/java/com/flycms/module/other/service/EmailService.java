/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */
package com.flycms.module.other.service;

import java.security.Security;
import java.util.*;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
 * 发送邮件的测试程序
 * 
 * @author lwq
 * 
 */
@Service
public class EmailService {
	@Autowired
	protected ConfigService configService;
    @Autowired
    protected EmailDao emailDao;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 给指定用户邮箱发送重置密码邮件
     *
     * @param userEmail
     *        需要发送的用户信息
     * @param code
     *        需要发送的验证码
     * @param tpCode
     *        后台设置的邮件模板key
     * @return
     * @throws MessagingException
     */
    @SuppressWarnings("restriction")
    public void sendEmail(String userEmail,String code, String tpCode) throws MessagingException {
        Email email=emailDao.findEmailTempletByTpCode(tpCode);
        Map<String, String> map = new HashMap<String, String>();
        map.put("code",code);
        map.put("userEmail",userEmail);
        map.put("createTime", DateUtils.getTime());
        System.out.println(PlaceholderUtils.resolvePlaceholders(email.getContent(), map));
        String mailBody =PlaceholderUtils.resolvePlaceholders(email.getContent(), map);
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        // 配置发送邮件的环境属性
        final Properties props = new Properties();
        /*
         * 可用的属性： mail.store.protocol / mail.transport.protocol / mail.host /
         * mail.user / mail.from
         */
        // 表示SMTP发送邮件，需要进行身份验证
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", configService.getStringByKey("fly_smtp_server"));
        // 发件人的账号
        props.put("mail.user", configService.getStringByKey("fly_smtp_usermail"));
        // 访问SMTP服务时需要提供的密码
        props.put("mail.password", configService.getStringByKey("fly_smtp_password"));
        props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.put("mail.smtp.port", configService.getStringByKey("fly_smtp_port"));
        props.put("mail.smtp.socketFactory.port", configService.getStringByKey("fly_smtp_port"));
        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.starttls.enable", "true");
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        Address form = new InternetAddress(props.getProperty("mail.user"));
        message.setFrom(form);

        Address toAddress=null;
        String[] mailTo=new String[]{userEmail};
        for(int i =0; i<mailTo.length; i++){
            toAddress = new InternetAddress(mailTo[i]);
            message.setRecipient(RecipientType.TO, toAddress);
        }
        // 设置收件人


        // 设置明抄送
        //InternetAddress cc = new InternetAddress("79678111@qq.com");
        // message.setRecipient(RecipientType.CC, cc);

        // 设置密送，其他的收件人不能看到密送的邮件地址
        InternetAddress bcc = new InternetAddress("79678111@qq.com");
        message.setRecipient(RecipientType.BCC, bcc);

        // 设置邮件标题
        message.setSubject(email.getTitle() == null ? "开源之家测试邮件" : email.getTitle(), "GBK"); // 设置邮件主题

        // 设置邮件的内容体
        message.setText("<html><head><meta charset='utf-8'></head><body>" + mailBody + "</body></html>", "GBK", "html");

        // 设置邮件发送日期
        message.setSentDate(new Date());

        // 发送邮件
        Transport.send(message);
    }

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

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