package com.the198thstreet.news.google.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleHeadlineRssClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineRssClient.class);
    private final RestTemplate restTemplate;

    public GoogleHeadlineRssClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchRss(String rssUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(rssUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("구글 RSS 호출 실패 - status={}", response.getStatusCode());
                return null;
            }
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("구글 RSS 호출 중 오류 발생: {}", ex.getMessage());
            return null;
        }
    }
}
