package com.the198thstreet.news.model;

/**
 * 클라이언트에 전달될 뉴스 아이템 표현.
 * <p>
 * DTO를 따로 두지 않고 Map 대신 레코드를 사용해, 필드 의미를 명확히 남기면서도
 * 불변성을 보장한다.
 */
public record NewsItem(
        String title,          // HTML 태그를 포함할 수 있는 제목
        String originallink,   // 언론사 원문 링크
        String link,           // 네이버 뉴스 조회 링크
        String description,    // 요약 혹은 미리보기 텍스트
        String pubDate         // RFC 1123 형식의 발행 시각 문자열
) {
}
