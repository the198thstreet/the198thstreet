package com.the198thstreet.news.controller;

import com.the198thstreet.news.model.NewsSearchResponse;
import com.the198thstreet.news.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public NewsSearchResponse listNews(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "sort", defaultValue = "sim") String sort) {

        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력해 주세요.");
        }

        return newsService.fetchNews(query, sort);
    }
}
