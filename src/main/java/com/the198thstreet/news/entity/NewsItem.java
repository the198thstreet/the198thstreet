package com.the198thstreet.news.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "news_item")
@Getter
@Setter
@NoArgsConstructor
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    @JsonIgnore
    private NewsSearchResult result;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2048)
    private String originallink;

    @Column(nullable = false, length = 2048)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "pub_date_raw", nullable = false, length = 50)
    private String pubDateRaw;

    @Column(name = "pub_date_dt")
    private LocalDateTime pubDateDt;
}
