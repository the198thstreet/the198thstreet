package com.the198thstreet.news.google.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 구글 RSS URL을 GET 호출해 XML 문자열을 그대로 반환하는 매우 단순한 클라이언트입니다.
 * <p>
 * 별도의 변환이나 인증이 없으므로 try-catch 로 실패만 잡고, 성공하면 body 문자열을 그대로 돌려줍니다.
 */
@Component
public class GoogleHeadlineRssClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineRssClient.class);
    private final RestTemplate restTemplate;

    public GoogleHeadlineRssClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 지정한 RSS URL을 호출합니다.
     * @param rssUrl google.headline.rss-url 프로퍼티에서 읽어온 값
     * @return 성공 시 응답 본문(XML 문자열), 실패 시 null
     */
    public String fetchRss(String rssUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(rssUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                // HTTP 200 범위가 아니면 경고 로그만 남기고 null 반환
                log.warn("구글 RSS 호출 실패 - status={}", response.getStatusCode());
                return null;
            }
            return response.getBody();
        } catch (RestClientException ex) {
            // 네트워크 오류 등 예외 발생 시에도 애플리케이션이 죽지 않도록 null 반환
            log.warn("구글 RSS 호출 중 오류 발생: {}", ex.getMessage());
            return null;
        }
    }
}
