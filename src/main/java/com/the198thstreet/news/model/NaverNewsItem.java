package com.the198thstreet.news.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 네이버 뉴스 검색 API의 단일 결과를 역직렬화하기 위한 모델.
 * <p>
 * Naver API 응답은 때때로 추가 필드가 붙을 수 있어 {@link JsonIgnoreProperties}로
 * 모르는 필드를 무시한다. 받은 데이터는 이후 {@link com.the198thstreet.news.mapper.NewsResponseMapper}
 * 를 통해 클라이언트에 노출되는 {@link NewsItem} 형태로 변환된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsItem(
        @JsonProperty("title") String title,
        @JsonProperty("originallink") String originallink,
        @JsonProperty("link") String link,
        @JsonProperty("description") String description,
        @JsonProperty("pubDate") String pubDate
) {
}
