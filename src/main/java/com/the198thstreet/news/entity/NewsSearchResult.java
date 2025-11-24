package com.the198thstreet.news.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 뉴스 검색 결과 한 건을 표현하는 단순 POJO.
 * <p>
 * JPA 대신 MyBatis로 매핑하기 때문에 불필요한 어노테이션을 모두 제거했고,
 * 컬럼명은 DB와 1:1로 매핑되도록 camelCase 필드명을 그대로 사용합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class NewsSearchResult {

    /** 뉴스 검색 결과 PK (result_id). */
    private Long id;

    /** lastBuildDate 원본 문자열. */
    private String lastBuildRaw;

    /** lastBuildDate 파싱 값(옵션). */
    private LocalDateTime lastBuildDt;

    /** 검색 결과 전체 건수 total. */
    private Long totalCount;

    /** 검색 시작 위치 start. */
    private Integer startNo;

    /** 한 번에 표시한 건수 display. */
    private Integer displayCount;

    /** 검색 결과에 포함된 기사 목록. */
    private List<NewsItem> items = new ArrayList<>();

    public void addItem(NewsItem item) {
        item.setResultId(this.id);
        this.items.add(item);
    }
}
