package com.the198thstreet.news.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "news_search_result")
@Getter
@Setter
@NoArgsConstructor
public class NewsSearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @Column(name = "last_build_raw", nullable = false, length = 50)
    private String lastBuildRaw;

    @Column(name = "last_build_dt")
    private LocalDateTime lastBuildDt;

    @Column(name = "total_count", nullable = false)
    private Long totalCount;

    @Column(name = "start_no", nullable = false)
    private Integer startNo;

    @Column(name = "display_count", nullable = false)
    private Integer displayCount;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewsItem> items = new ArrayList<>();

    public void addItem(NewsItem item) {
        items.add(item);
        item.setResult(this);
    }
}
