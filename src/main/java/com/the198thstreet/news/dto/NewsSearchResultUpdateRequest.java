package com.the198thstreet.news.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsSearchResultUpdateRequest {

    @NotBlank
    @Size(max = 50)
    private String lastBuildRaw;

    private String lastBuildDt; // optional ISO date-time string

    @Min(0)
    private Long totalCount;

    @Min(1)
    private Integer startNo;

    @Min(1)
    private Integer displayCount;
}
