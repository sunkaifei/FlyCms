package com.flycms.module.user.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    //用户id
    private Long userId;
    //短域名
    private String shortUrl;
    //用户名,可做登录使用
    private String userName;
    //手机号码,可做登录使用
    private String userMobile;
    //联系邮箱,可做登录使用
    private String userEmail;
    //密码
    private String password;
    //确认密码
    private String repassword;
    //用户昵称
    private String nickName;
    //姓名
    private String trueName;
    //用户头像
    private String avatar;
    //个性签名
    private String signature;
    //工作职业
    private String work;
    //自我介绍
    private String description;
    //座机号码
    private String telephone;
    //省份id
    private Integer province;
    //地区id
    private Integer city;
    //县市id
    private Integer area;
    //详细地址
    private String contactAddr;
    //邮政编码
    private String zip;
    //联系qq号码
    private String qq;
    //性别
    private Integer sex;
    //生日
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private Date birthday;
    //所在的会员组
        private Long groupId;
    //经验值
    private Integer exp;
    //所有提的问题数量
    private Integer countQuestion;
    //所有关注的问题数量
    private Integer countQuestionFollw;
    //发布文章数量
    private Integer countArticle;
    //所有关注话题的数量
    private Integer countTopic;
    //所有回答的数量
    private Integer countAnswer;
    //关注别人的数量
    private Integer countFollw;
    //所有的粉丝数量
    private Integer countFans;
    //注册时间
    private Date createTime;
    //账户剩余金额
    private BigDecimal balance;
    //积分
    private Integer score;
    //成功登陆最后时间
    private Date lastLogin;
    private String custom;
    //成功登陆最后ip
    private String loginIp;
    //尝试登陆次数
    private Integer attempts;
    //尝试登陆最后时间
    private Date attemptsTime;
    //审核状态
    private Integer status;
    //用户sessionKey
    private String sessionKey;
}
