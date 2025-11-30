package com.the198thstreet.news.google.parser;

import java.time.LocalDateTime;

public class GoogleHeadlineArticle {
    private String topicGuid;
    private String topicTitle;
    private String topicLink;
    private String topicSourceName;
    private String topicSourceUrl;
    private LocalDateTime topicPubDate;
    private Integer articleOrder;
    private String articleTitle;
    private String articleUrl;
    private String articlePressName;

    public String getTopicGuid() {
        return topicGuid;
    }

    public void setTopicGuid(String topicGuid) {
        this.topicGuid = topicGuid;
    }

    public String getTopicTitle() {
        return topicTitle;
    }

    public void setTopicTitle(String topicTitle) {
        this.topicTitle = topicTitle;
    }

    public String getTopicLink() {
        return topicLink;
    }

    public void setTopicLink(String topicLink) {
        this.topicLink = topicLink;
    }

    public String getTopicSourceName() {
        return topicSourceName;
    }

    public void setTopicSourceName(String topicSourceName) {
        this.topicSourceName = topicSourceName;
    }

    public String getTopicSourceUrl() {
        return topicSourceUrl;
    }

    public void setTopicSourceUrl(String topicSourceUrl) {
        this.topicSourceUrl = topicSourceUrl;
    }

    public LocalDateTime getTopicPubDate() {
        return topicPubDate;
    }

    public void setTopicPubDate(LocalDateTime topicPubDate) {
        this.topicPubDate = topicPubDate;
    }

    public Integer getArticleOrder() {
        return articleOrder;
    }

    public void setArticleOrder(Integer articleOrder) {
        this.articleOrder = articleOrder;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    public String getArticlePressName() {
        return articlePressName;
    }

    public void setArticlePressName(String articlePressName) {
        this.articlePressName = articlePressName;
    }
}
