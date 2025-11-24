package com.the198thstreet.news.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.the198thstreet.news.service.NewsService;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/searchResult/news/{query}/{sort}")
    public Map<String, Object> searchNews(@PathVariable String query, @PathVariable String sort) {
        return newsService.searchNews(query, sort);
    }
}
