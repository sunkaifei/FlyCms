package com.flycms.module.weight.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 权重实体类
 * 参照：https://blog.csdn.net/zhuhengv/article/details/50475925
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 16:31 2018/10/26
 */
@Setter
@Getter
public class Weight implements Serializable {
    private static final long serialVersionUID = 1L;
    //Qanswers表示回答的数量，代表有多少人参与这个问题。这个值越大，得分将成倍放大。这里需要注意的是，如果无人回答，Qanswers就等于0，这时Qscore再高也没用，意味着再好的问题，也必须有人回答，否则进不了热点问题排行榜。
    private int Qanswer;
    //Qscore(问题得分)= 赞成票-反对票。如果某个问题越受到好评，排名自然应该越靠前。
    private int Qscore;
    //回答得分,一般来说，"回答"比"问题"更有意义。这一项的得分越高，就代表回答的质量越高。
    private int Ascores;
    //浏览次数越多，就代表越受关注，得分也就越高。这里使用了以10为底的对数，用意是当访问量越来越大，它对得分的影响将不断变小。
    private int Qview;
    //文章已发布时间
    private long Qage;
    //最新回帖时间
    private long Qupdated;
}
