package com.flycms.module.weight.service;

import com.flycms.module.article.model.Article;
import com.flycms.module.article.model.ArticleComment;
import com.flycms.module.article.model.ArticleCount;
import com.flycms.module.article.service.ArticleService;
import com.flycms.module.question.model.Question;
import com.flycms.module.question.service.QuestionService;
import com.flycms.module.share.service.ShareService;
import com.flycms.module.weight.model.Weight;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 权重统一处理服务
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 16:23 2018/10/26
 */
@Service
public class WeightService {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private QuestionService questionService;

    //更新文章权重
    public void updateArticleWeight(){
        List<SolrInputDocument> docs = new ArrayList<>();
        int j=articleService.getArticleIndexCount();
        int c = j%1000==0?j/1000:j/1000+1;
        for (int i=0; i<c;i++) {
            List<Article> list = articleService.getArticleIndexList(i,1000);
            for (Article info:list) {
                Article article=articleService.findArticleById(info.getId(),2);
                ArticleCount count=articleService.findArticleCountById(info.getId());
                Weight weight=new Weight();
                List<ArticleComment> comments = articleService.getArticleCommentByArticleId(article.getId());
                Optional<Integer> Ascore = Optional.of(0);
                if (comments.size() > 0) {
                    Ascore = comments.stream()
                            .map(comment -> comment.getCountDigg() - comment.getCountBurys())
                            .reduce(Integer::sum);
                }
                weight.setAscores(Ascore.get());
                weight.setQview(count.getCountView());
                weight.setQscore((count.getCountDigg() == null ? 0 :count.getCountDigg()) - (count.getCountBurys() == null ? 0 :count.getCountBurys()));
                weight.setQanswer(count.getCountComment());
                //最新更新时间
                weight.setQage((new Date().getTime()-article.getCreateTime().getTime())/ (1000 * 60 * 60 * 24));
                long Qupdated = 0;
                //查询最新更新评论时间
                ArticleComment comment = articleService.findNewestArticleById(article.getId());
                if(comment!=null){
                    Qupdated = (new Date().getTime()-comment.getCreateTime().getTime())/ (1000 * 60 * 60 * 24);
                }
                weight.setQupdated(Qupdated);
                //更新积分
                articleService.updateArticleWeight(this.weight(weight),article.getId());
            }
        }
    }

    /**
     * 计算话题的weight
     *
     * Stack Overflow热点问题的排名，与参与度(Qviews和Qanswers)和质量(Qscore和Ascores)成正比，与时间(Qage和Qupdated)成反比。
     *
     * @param weight
     */
    public double weight(Weight weight) {
        //Math.pow(((Qage / 2) + (Qupdated/2)+1), 1.5),Qage和Qupdated的单位都是天。如果一个问题的存在时间越久，或者距离上一次回答的时间越久，Qage和Qupdated的值就相应增大。
        double weightScore = ((weight.getQview() * 4) + (weight.getQanswer() * weight.getQscore()) / 5 + weight.getAscores()) / Math.pow(((weight.getQage() / 2) + (weight.getQupdated()/2)+1), 1.5);
        return weightScore;
    }
}
