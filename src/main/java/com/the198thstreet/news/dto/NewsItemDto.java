package com.the198thstreet.news.dto;

import com.the198thstreet.news.entity.NewsItem;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsItemDto {
    private Long id;
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDateRaw;
    private LocalDateTime pubDateDt;

    public static NewsItemDto fromEntity(NewsItem item) {
        return NewsItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .originallink(item.getOriginallink())
                .link(item.getLink())
                .description(item.getDescription())
                .pubDateRaw(item.getPubDateRaw())
                .pubDateDt(item.getPubDateDt())
                .build();
    }
}
