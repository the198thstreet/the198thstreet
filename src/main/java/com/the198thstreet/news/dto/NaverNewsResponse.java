package com.the198thstreet.news.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NaverNewsResponse {
    private String lastBuildDate;
    private long total;
    private int start;
    private int display;
    private List<NaverNewsItemResponse> items;
}
