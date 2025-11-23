package com.the198thstreet.news.dto;

import com.the198thstreet.news.entity.NewsSearchResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsSearchResultDto {
    private Long id;
    private String lastBuildRaw;
    private LocalDateTime lastBuildDt;
    private Long totalCount;
    private Integer startNo;
    private Integer displayCount;
    private List<NewsItemDto> items;

    public static NewsSearchResultDto fromEntity(NewsSearchResult result) {
        return NewsSearchResultDto.builder()
                .id(result.getId())
                .lastBuildRaw(result.getLastBuildRaw())
                .lastBuildDt(result.getLastBuildDt())
                .totalCount(result.getTotalCount())
                .startNo(result.getStartNo())
                .displayCount(result.getDisplayCount())
                .items(result.getItems().stream()
                        .map(NewsItemDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}
