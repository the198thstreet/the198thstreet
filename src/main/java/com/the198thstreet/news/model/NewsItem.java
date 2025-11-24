package com.the198thstreet.news.model;

public record NewsItem(
        String title,
        String originallink,
        String link,
        String description,
        String pubDate
) {
}
