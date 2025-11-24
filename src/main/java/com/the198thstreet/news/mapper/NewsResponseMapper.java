package com.the198thstreet.news.mapper;

import com.the198thstreet.news.model.NaverNewsItem;
import com.the198thstreet.news.model.NaverNewsResponse;
import com.the198thstreet.news.model.NewsItem;
import com.the198thstreet.news.model.NewsSearchResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class NewsResponseMapper {

    private NewsResponseMapper() {
    }

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
