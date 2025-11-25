package com.the198thstreet.news.model;

import java.util.List;

/**
 * 클라이언트에게 반환되는 뉴스 검색 결과 전체 응답.
 * <p>
 * Map 대신 레코드를 사용해 응답 구조를 명확히 드러내면서, 불변 객체로 관리한다.
 */
public record NewsSearchResponse(
        String lastBuildDate, // 응답 생성 시각
        int total,            // 전체 검색 건수
        int start,            // 시작 위치
        int display,          // 전달된 건수
        List<NewsItem> items  // 정제된 뉴스 아이템 목록
) {
}
