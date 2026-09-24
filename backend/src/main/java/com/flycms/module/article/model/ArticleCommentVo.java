package com.flycms.module.article.model;

/**
 * 评论审核列表 VO（规划阶段 B1）
 *
 * 列表需要展示「评论所属文章标题」，且需按真实审核状态过滤，
 * 因此单独出 VO 承载 join 出来的字段，不污染 ArticleComment 实体。
 *
 * status 语义沿用 fly_article_comment：0未审 1正常 2未通过 3删除
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class ArticleCommentVo extends ArticleComment {
    private static final long serialVersionUID = 1L;

    /** 评论所属文章标题 */
    private String articleTitle;

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }
}
