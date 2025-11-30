package com.the198thstreet.news.google.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "NEWS_GOOGLE_HEADLINE",
        uniqueConstraints = @UniqueConstraint(name = "UK_NEWS_GOOGLE_ARTICLE_URL", columnNames = "ARTICLE_URL"),
        indexes = {
                @Index(name = "IDX_NEWS_GOOGLE_PUBDATE", columnList = "TOPIC_PUB_DATE"),
                @Index(name = "IDX_NEWS_GOOGLE_PRESS", columnList = "ARTICLE_PRESS_NAME"),
                @Index(name = "IDX_NEWS_GOOGLE_TOPIC", columnList = "TOPIC_GUID")
        })
public class NewsGoogleHeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOPIC_GUID", nullable = false, length = 255)
    private String topicGuid;

    @Column(name = "TOPIC_TITLE", nullable = false, length = 500)
    private String topicTitle;

    @Column(name = "TOPIC_LINK", nullable = false, length = 1000)
    private String topicLink;

    @Column(name = "TOPIC_SOURCE_NAME", length = 200)
    private String topicSourceName;

    @Column(name = "TOPIC_SOURCE_URL", length = 500)
    private String topicSourceUrl;

    @Column(name = "TOPIC_PUB_DATE", nullable = false)
    private LocalDateTime topicPubDate;

    @Column(name = "ARTICLE_ORDER", nullable = false)
    private Integer articleOrder;

    @Column(name = "ARTICLE_TITLE", nullable = false, length = 500)
    private String articleTitle;

    @Column(name = "ARTICLE_URL", nullable = false, length = 1000)
    private String articleUrl;

    @Column(name = "ARTICLE_PRESS_NAME", length = 200)
    private String articlePressName;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
