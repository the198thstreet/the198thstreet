package com.the198thstreet.news.model;

import java.util.List;

public record NewsSearchResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NewsItem> items
) {
}
