package com.the198thstreet.news.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsItem(
        @JsonProperty("title") String title,
        @JsonProperty("originallink") String originallink,
        @JsonProperty("link") String link,
        @JsonProperty("description") String description,
        @JsonProperty("pubDate") String pubDate
) {
}
