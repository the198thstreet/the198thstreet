package com.the198thstreet.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsItemUpdateRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 2048)
    private String originallink;

    @NotBlank
    @Size(max = 2048)
    private String link;

    private String description;

    @NotBlank
    @Size(max = 50)
    private String pubDateRaw;
}
