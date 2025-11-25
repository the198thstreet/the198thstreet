package com.the198thstreet.news.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mybatis.spring.SqlSessionTemplate;
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
    private final SqlSessionTemplate sqlSessionTemplate;

    @Value("${naver.api.client-id}")
    private String clientId; // 네이버 API 클라이언트 ID (헤더로 전달)

    @Value("${naver.api.client-secret}")
    private String clientSecret; // 네이버 API 클라이언트 비밀키

    @Value("${naver.api.news-url}")
    private String newsApiUrl; // 뉴스 검색 API 엔드포인트

    public NewsService(RestTemplate restTemplate, SqlSessionTemplate sqlSessionTemplate) {
        this.restTemplate = restTemplate;
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * 검색어와 정렬 옵션으로 네이버 뉴스 목록을 조회한다.
     *
     * @param query   검색어(필수)
     * @param sort    정렬 방식(sim: 유사도, date: 날짜). 비어있으면 기본값 sim 사용
     * @param display 한 번에 가져올 건수(1~100). 비어 있으면 네이버 기본값을 따름
     * @param start   몇 번째 결과부터 가져올지(1~1000). 비어 있으면 네이버 기본값을 따름
     * @return 클라이언트에 바로 전달 가능한 뉴스 검색 응답
     */
    public Map<String, Object> fetchNews(String query, String sort, Integer display, Integer start) {
        // 정렬 파라미터가 비어 있으면 기본값인 sim(유사도 순)으로 고정한다.
        String normalizedSort = sort == null || sort.isBlank() ? "sim" : sort;
        int normalizedDisplay = display == null ? 10 : display;
        int normalizedStart = start == null ? 1 : start;

        // UTF-8 로 인코딩된 요청 URI 구성: query, sort 파라미터를 명시적으로 추가한다.
        URI requestUri = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("query", query)
                .queryParam("sort", normalizedSort)
                .queryParam("display", normalizedDisplay)
                .queryParam("start", normalizedStart)
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
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<>() {});

        Map<String, Object> mappedResponse = normalizeResponse(response.getBody());

        // 네이버 API가 반환한 아이템 중 제목이 중복되지 않는 건만 DB에 적재한다.
        persistNewsItems(mappedResponse, query, normalizedSort, normalizedDisplay, normalizedStart);

        return mappedResponse;
    }

    /**
     * 네이버 API에서 받아온 뉴스 데이터를 MyBatis를 통해 DB에 저장한다.
     * <p>
     * - MyBatis XML 매퍼(NewsMapper.xml)에 정의된 구문을 {@link SqlSessionTemplate}으로 직접 호출한다.<br>
     * - 제목이 이미 존재하는 경우 INSERT를 수행하지 않아 중복 레코드를 방지한다.<br>
     * - 별도의 자바 매퍼 인터페이스 없이 가장 단순한 방식으로 쿼리를 실행한다.
     *
     * @param response  외부 API 응답 맵
     * @param query     검색어(저장 메타데이터)
     * @param sort      정렬 방식(저장 메타데이터)
     * @param display   한 번에 요청한 건수
     * @param start     시작 위치
     */
    private void persistNewsItems(Map<String, Object> response, String query, String sort, int display, int start) {
        Object itemsObj = response.get("items");
        if (!(itemsObj instanceof List<?> rawItems) || rawItems.isEmpty()) {
            // 저장할 데이터가 없다면 조용히 종료한다.
            return;
        }

        for (Object itemObj : rawItems) {
            if (!(itemObj instanceof Map<?, ?> item)) {
                continue;
            }

            String title = asString(item.get("title"));
            if (title.isEmpty()) {
                continue;
            }

            // 1) 제목이 이미 저장되어 있는지 카운트 쿼리로 확인한다.
            int existingCount = sqlSessionTemplate.selectOne("NewsMapper.countByTitle", title);
            if (existingCount > 0) {
                // 동일한 제목이 있으면 다음 아이템으로 건너뛴다.
                continue;
            }

            // 2) 삽입에 필요한 필드를 맵으로 조립한다.
            Map<String, Object> params = new HashMap<>();
            params.put("title", title);
            params.put("originallink", asString(item.get("originallink")));
            params.put("link", asString(item.get("link")));
            params.put("description", asString(item.get("description")));
            params.put("pubDate", asString(item.get("pubDate")));
            params.put("query", query);
            params.put("sort", sort);
            params.put("display", display);
            params.put("start", start);

            // 3) XML 매퍼에 정의된 insert 구문을 실행한다.
            sqlSessionTemplate.insert("NewsMapper.insertNewsItem", params);
        }
    }

    /**
     * 외부 API 응답을 Map 기반 구조로 정규화한다.
     *
     * @param response 네이버 API 응답 바디
     * @return lastBuildDate, total, start, display, items 키를 포함한 맵
     */
    private Map<String, Object> normalizeResponse(Map<String, Object> response) {
        Map<String, Object> normalized = new HashMap<>();
        Map<String, Object> safeResponse = response == null ? Map.of() : response;

        normalized.put("lastBuildDate", asString(safeResponse.get("lastBuildDate")));
        normalized.put("total", asInt(safeResponse.get("total")));
        normalized.put("start", asInt(safeResponse.get("start")));
        normalized.put("display", asInt(safeResponse.get("display")));
        normalized.put("items", extractItems(safeResponse.get("items")));

        return normalized;
    }

    private List<Map<String, Object>> extractItems(Object itemsObj) {
        if (!(itemsObj instanceof List<?> rawItems)) {
            return List.of();
        }

        return rawItems.stream()
                .filter(Map.class::isInstance)
                .map(item -> ((Map<?, ?>) item).entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> Objects.toString(entry.getKey(), ""),
                                entry -> entry.getValue(),
                                (first, second) -> first,
                                HashMap::new)))
                .toList();
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
