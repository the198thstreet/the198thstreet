package com.the198thstreet.news.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsResponse(
        @JsonProperty("lastBuildDate") String lastBuildDate,
        @JsonProperty("total") int total,
        @JsonProperty("start") int start,
        @JsonProperty("display") int display,
        @JsonProperty("items") List<NaverNewsItem> items
) {
}
