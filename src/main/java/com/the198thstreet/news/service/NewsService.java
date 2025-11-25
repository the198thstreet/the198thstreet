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

/**
 * 네이버 뉴스 검색 API 호출과 응답 변환을 담당하는 서비스.
 * <p>
 * 컨트롤러에서는 검증만 수행하고, 실제 외부 API 요청과 변환 로직은 이 서비스에서 처리한다.
 */
@Service
public class NewsService {

    private final RestTemplate restTemplate;

    @Value("${naver.api.client-id}")
    private String clientId; // 네이버 API 클라이언트 ID (헤더로 전달)

    @Value("${naver.api.client-secret}")
    private String clientSecret; // 네이버 API 클라이언트 비밀키

    @Value("${naver.api.news-url}")
    private String newsApiUrl; // 뉴스 검색 API 엔드포인트

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 검색어와 정렬 옵션으로 네이버 뉴스 목록을 조회한다.
     *
     * @param query 검색어(필수)
     * @param sort  정렬 방식(sim: 유사도, date: 날짜). 비어있으면 기본값 sim 사용
     * @return 클라이언트에 바로 전달 가능한 뉴스 검색 응답
     */
    public NewsSearchResponse fetchNews(String query, String sort) {
        // 정렬 파라미터가 비어 있으면 기본값인 sim(유사도 순)으로 고정한다.
        String normalizedSort = sort == null || sort.isBlank() ? "sim" : sort;

        // UTF-8 로 인코딩된 요청 URI 구성: query, sort 파라미터를 명시적으로 추가한다.
        URI requestUri = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("query", query)
                .queryParam("sort", normalizedSort)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        // 네이버 API 인증 헤더 설정 및 UTF-8 수용 가능 여부 명시
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        // 바디는 필요 없으므로 HttpEntity<Void> 로 헤더만 실어 보낸다.
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<NaverNewsResponse> response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<>() {});

        // 외부 응답을 내부 응답 모델로 변환하여 반환한다.
        return NewsResponseMapper.toNewsSearchResponse(response.getBody());
    }
}
