package com.the198thstreet.news.controller;

import com.the198thstreet.news.dto.NewsItemDto;
import com.the198thstreet.news.dto.NewsItemUpdateRequest;
import com.the198thstreet.news.dto.NewsSearchResultDto;
import com.the198thstreet.news.dto.NewsSearchResultUpdateRequest;
import com.the198thstreet.news.service.NaverNewsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NaverNewsService naverNewsService;

    public NewsController(NaverNewsService naverNewsService) {
        this.naverNewsService = naverNewsService;
    }

    @GetMapping("/searchResult/news/{query}/{sort}")
    public NewsSearchResultDto fetchAndSave(@PathVariable String query, @PathVariable String sort) {
        return naverNewsService.fetchAndSave(query, sort);
    }

    @GetMapping("/results")
    public List<NewsSearchResultDto> findAllResults() {
        return naverNewsService.findAllResults();
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<NewsSearchResultDto> findResult(@PathVariable Long id) {
        return naverNewsService.findResult(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/results/{id}")
    public ResponseEntity<NewsSearchResultDto> updateResult(
            @PathVariable Long id,
            @Valid @RequestBody NewsSearchResultUpdateRequest request) {
        return naverNewsService.updateResult(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/results/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable Long id) {
        naverNewsService.deleteResult(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<NewsItemDto> findItem(@PathVariable Long id) {
        return naverNewsService.findItem(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<NewsItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody NewsItemUpdateRequest request) {
        return naverNewsService.updateItem(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        naverNewsService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
