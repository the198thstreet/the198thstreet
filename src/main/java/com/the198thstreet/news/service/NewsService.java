package com.the198thstreet.news.service;

import com.the198thstreet.news.mapper.NewsResponseMapper;
import com.the198thstreet.news.model.NewsSearchResponse;
import com.the198thstreet.news.model.NaverNewsResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsService {

    private final RestTemplate restTemplate;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.news-url}")
    private String newsApiUrl;

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public NewsSearchResponse fetchNews(String query, String sort) {
        String normalizedSort = sort == null || sort.isBlank() ? "sim" : sort;

        URI requestUri = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("query", query)
                .queryParam("sort", normalizedSort)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<NaverNewsResponse> response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<>() {});

        return NewsResponseMapper.toNewsSearchResponse(response.getBody());
    }
}
