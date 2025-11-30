package com.the198thstreet.news.google.web.dto;

import java.time.LocalDateTime;

import com.the198thstreet.news.google.entity.NewsGoogleHeadline;

public class NewsHeadlineDto {
    private Long id;
    private String topicTitle;
    private LocalDateTime topicPubDate;
    private String articleTitle;
    private String articleUrl;
    private String articlePressName;

    public static NewsHeadlineDto fromEntity(NewsGoogleHeadline entity) {
        NewsHeadlineDto dto = new NewsHeadlineDto();
        dto.setId(entity.getId());
        dto.setTopicTitle(entity.getTopicTitle());
        dto.setTopicPubDate(entity.getTopicPubDate());
        dto.setArticleTitle(entity.getArticleTitle());
        dto.setArticleUrl(entity.getArticleUrl());
        dto.setArticlePressName(entity.getArticlePressName());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopicTitle() {
        return topicTitle;
    }

    public void setTopicTitle(String topicTitle) {
        this.topicTitle = topicTitle;
    }

    public LocalDateTime getTopicPubDate() {
        return topicPubDate;
    }

    public void setTopicPubDate(LocalDateTime topicPubDate) {
        this.topicPubDate = topicPubDate;
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
