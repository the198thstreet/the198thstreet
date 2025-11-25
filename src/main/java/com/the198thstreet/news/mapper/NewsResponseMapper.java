package com.the198thstreet.news.mapper;

import com.the198thstreet.news.model.NaverNewsItem;
import com.the198thstreet.news.model.NaverNewsResponse;
import com.the198thstreet.news.model.NewsItem;
import com.the198thstreet.news.model.NewsSearchResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 외부 API 응답 객체를 클라이언트 응답 형태로 변환하는 정적 유틸리티.
 * <p>
 * 서비스 계층에서 변환 로직을 분리해 두면, 응답 스펙이 바뀌더라도 이 클래스만 수정하면 되어
 * 응집도가 높아진다.
 */
public final class NewsResponseMapper {

    private NewsResponseMapper() {
    }

    /**
     * 네이버 뉴스 응답을 내부에서 사용하는 {@link NewsSearchResponse}로 변환한다.
     *
     * @param response 네이버 API 응답 객체, null 일 수도 있다.
     * @return 필드가 채워진 {@link NewsSearchResponse}, 응답이 null 이면 비어있는 객체를 반환
     */
    public static NewsSearchResponse toNewsSearchResponse(NaverNewsResponse response) {
        if (response == null) {
            return new NewsSearchResponse("", 0, 0, 0, Collections.emptyList());
        }

        List<NewsItem> items = toNewsItems(response.items());

        return new NewsSearchResponse(
                response.lastBuildDate(),
                response.total(),
                response.start(),
                response.display(),
                items
        );
    }

    /**
     * 네이버 뉴스 아이템 리스트를 클라이언트 응답용 리스트로 변환한다.
     *
     * @param items 네이버에서 받은 뉴스 아이템 목록
     * @return 빈 리스트 또는 변환된 뉴스 아이템 리스트
     */
    private static List<NewsItem> toNewsItems(List<NaverNewsItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(item -> new NewsItem(
                        item.title(),
                        item.originallink(),
                        item.link(),
                        item.description(),
                        item.pubDate()))
                .collect(Collectors.toList());
    }
}
