package com.the198thstreet.news.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 네이버 뉴스 검색 API의 전체 응답을 매핑하는 모델.
 * <p>
 * API의 페이징 정보와 뉴스 아이템 목록을 그대로 담아두고,
 * 이후 서비스 계층에서 클라이언트 응답 모델로 변환한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsResponse(
        @JsonProperty("lastBuildDate") String lastBuildDate, // 응답이 생성된 시각
        @JsonProperty("total") int total,                    // 전체 검색 결과 수
        @JsonProperty("start") int start,                    // 요청한 시작 위치
        @JsonProperty("display") int display,                // 이번 응답에 포함된 건수
        @JsonProperty("items") List<NaverNewsItem> items     // 뉴스 목록
) {
}
