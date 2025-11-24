package com.the198thstreet.news.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 단일 뉴스 기사를 담는 POJO.
 * <p>
 * MyBatis 매핑 시 resultId 컬럼으로 상위 검색 결과를 연결합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class NewsItem {

    /** 뉴스 기사 PK (news_id). */
    private Long id;

    /** 상위 검색 결과 FK (result_id). */
    private Long resultId;

    /** 기사 제목(HTML 태그 포함 가능). */
    private String title;

    /** 원문 링크(옵션). */
    private String originallink;

    /** 네이버 등 최종 링크. */
    private String link;

    /** 기사 요약/설명. */
    private String description;

    /** pubDate 원본 문자열. */
    private String pubDateRaw;

    /** pubDate 파싱 값(옵션). */
    private LocalDateTime pubDateDt;
}
