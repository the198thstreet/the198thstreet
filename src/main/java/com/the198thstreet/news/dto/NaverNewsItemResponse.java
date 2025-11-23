package com.the198thstreet.news.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NaverNewsItemResponse {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;
}
