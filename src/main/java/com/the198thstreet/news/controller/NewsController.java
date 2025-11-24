package com.the198thstreet.news.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final RestTemplate restTemplate;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.news-url}")
    private String newsApiUrl;

    public NewsController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/searchResult/news/{query}/{sort}")
    public Map<String, Object> searchNews(@PathVariable String query, @PathVariable String sort) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("query", query)
                .queryParam("sort", sort);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        return response.getBody();
    }
}
