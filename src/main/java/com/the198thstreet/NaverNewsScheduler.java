package com.the198thstreet;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 네이버 뉴스 검색 OpenAPI를 1분마다 호출해 DB에 저장하는 매우 단순한 스케줄러.
 * <p>
 * 복잡한 엔티티나 서비스 계층 없이, 옛날식 JDBC 접근 방식과 {@link RestTemplate} 조합만 사용한다.
 */
@Component
public class NaverNewsScheduler {

        private static final Logger log = LoggerFactory.getLogger(NaverNewsScheduler.class);

        // start 파라미터를 순회할 고정 리스트. AtomicInteger를 이용해 순서를 계속 기억한다.
        private static final List<Integer> START_POSITIONS = Arrays.asList(1, 11, 21, 31, 41, 51, 61, 71, 81, 91);
        private static final DateTimeFormatter NAVER_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

        private final RestTemplate restTemplate;
        private final JdbcTemplate jdbcTemplate;
        private final AtomicInteger startIndex = new AtomicInteger(0);

        @Value("${naver.api.client-id}")
        private String clientId;

        @Value("${naver.api.client-secret}")
        private String clientSecret;

        @Value("${naver.api.news-url:https://openapi.naver.com/v1/search/news.json}")
        private String apiUrl;

        // API 호출 시 항상 동일하게 사용하는 고정 파라미터들
        private static final String FIXED_QUERY = "속보";
        private static final String FIXED_SORT = "sim";
        private static final int FIXED_DISPLAY = 100;

        public NaverNewsScheduler(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
                this.restTemplate = restTemplate;
                this.jdbcTemplate = jdbcTemplate;
        }

        /**
         * 1분마다 실행되며, start 값은 START_POSITIONS 목록을 순서대로 돌면서 사용한다.
         */
        @Scheduled(fixedRate = 60_000)
        public void callNaverNewsApi() {
                int index = startIndex.getAndUpdate(i -> (i + 1) % START_POSITIONS.size());
                int start = START_POSITIONS.get(index);

                // 호출 URL을 파라미터를 포함해 조립한다.
                String requestUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                                .queryParam("query", FIXED_QUERY)
                                .queryParam("sort", FIXED_SORT)
                                .queryParam("display", FIXED_DISPLAY)
                                .queryParam("start", start)
                                .toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Naver-Client-Id", clientId);
                headers.add("X-Naver-Client-Secret", clientSecret);

                try {
                        // REST 호출 수행
                        ResponseEntity<NaverNewsResponse> response = restTemplate.exchange(
                                        requestUrl,
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        NaverNewsResponse.class);

                        // 정상 응답이 아니면 경고 로그를 남기고 종료한다.
                        if (!response.getStatusCode().is2xxSuccessful()) {
                                log.warn("네이버 뉴스 API 호출 실패 - status: {} start: {}", response.getStatusCode(), start);
                                return;
                        }

                        NaverNewsResponse body = response.getBody();
                        if (body == null || CollectionUtils.isEmpty(body.getItems())) {
                                log.warn("네이버 뉴스 API 응답이 비어있음 - start: {}", start);
                                return;
                        }

                        // 받은 기사들을 DB에 하나씩 저장한다.
                        body.getItems().forEach(this::saveIfNewArticle);
                } catch (RestClientException restClientException) {
                        // 외부 API 요청 자체가 실패한 경우 경고 로그만 남긴다.
                        log.warn("네이버 뉴스 API 호출 도중 예외 발생 - start: {} message: {}", start, restClientException.getMessage());
                } catch (Exception unexpected) {
                        // 혹시 모를 다른 예외도 남겨두어 문제를 추적하기 쉽게 한다.
                        log.error("네이버 뉴스 스케줄러 처리 중 알 수 없는 오류 발생 - start: {}", start, unexpected);
                }
        }

        /**
         * 중복 여부를 간단히 검사한 뒤, 새로운 기사만 DB에 INSERT한다.
         * @param item 네이버 뉴스 응답 아이템
         */
        private void saveIfNewArticle(NewsItem item) {
                if (item == null) {
                        return;
                }

                // 이미 저장된 링크/원본링크와 겹치는지 검사
                Integer duplicateCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(1) FROM news_articles WHERE link = ? OR originallink = ?",
                                Integer.class,
                                item.getLink(),
                                item.getOriginallink());

                if (duplicateCount != null && duplicateCount > 0) {
                        // 겹치면 삽입하지 않는다.
                        log.debug("중복 기사 스킵 - title: {}", item.getTitle());
                        return;
                }

                // pubDate 문자열을 DATETIME으로 변환한다. 실패하면 null을 그대로 넣는다.
                Timestamp pubDateTimestamp = parsePubDate(item.getPubDate());

                jdbcTemplate.update(
                                "INSERT INTO news_articles (title, originallink, link, description, pub_date) VALUES (?,?,?,?,?)",
                                item.getTitle(),
                                item.getOriginallink(),
                                item.getLink(),
                                item.getDescription(),
                                pubDateTimestamp);
        }

        /**
         * 네이버 응답의 pubDate(예: "Tue, 28 Feb 2023 11:17:00 +0900")를 {@link Timestamp}로 변환한다.
         * @param pubDate 원본 문자열
         * @return 변환된 타임스탬프 또는 파싱 실패 시 null
         */
        private Timestamp parsePubDate(String pubDate) {
                if (pubDate == null || pubDate.isBlank()) {
                        return null;
                }
                try {
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, NAVER_DATE_FORMAT);
                        LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                        return Timestamp.valueOf(localDateTime);
                } catch (DateTimeParseException parseException) {
                        // 파싱 오류 시 에러 로그를 남기고 null을 반환한다.
                        log.error("pubDate 파싱 실패 - value: {} message: {}", pubDate, parseException.getMessage());
                        return null;
                }
        }

        /**
         * 네이버 뉴스 검색 API 응답을 Jackson이 매핑할 수 있도록 하는 DTO.
         * <p>필요한 필드만 남기고 매우 단순하게 구성했다.</p>
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NaverNewsResponse {
                private List<NewsItem> items;

                public List<NewsItem> getItems() {
                        return items;
                }

                public void setItems(List<NewsItem> items) {
                        this.items = items;
                }
        }

        /**
         * 네이버 뉴스 아이템 DTO. 카멜케이스와 스네이크 케이스가 자동 매핑되도록 필드명을 API 응답과 동일하게 맞춘다.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NewsItem {
                private String title;
                private String originallink;
                private String link;
                private String description;
                private String pubDate;

                public String getTitle() {
                        return title;
                }

                public void setTitle(String title) {
                        this.title = title;
                }

                public String getOriginallink() {
                        return originallink;
                }

                public void setOriginallink(String originallink) {
                        this.originallink = originallink;
                }

                public String getLink() {
                        return link;
                }

                public void setLink(String link) {
                        this.link = link;
                }

                public String getDescription() {
                        return description;
                }

                public void setDescription(String description) {
                        this.description = description;
                }

                public String getPubDate() {
                        return pubDate;
                }

                public void setPubDate(String pubDate) {
                        this.pubDate = pubDate;
                }

                @Override
                public String toString() {
                        return "NewsItem{" +
                                        "title='" + title + '\'' +
                                        ", originallink='" + originallink + '\'' +
                                        ", link='" + link + '\'' +
                                        ", description='" + description + '\'' +
                                        ", pubDate='" + pubDate + '\'' +
                                        '}';
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (!(o instanceof NewsItem newsItem)) return false;
                        return Objects.equals(link, newsItem.link) && Objects.equals(originallink, newsItem.originallink);
                }

                @Override
                public int hashCode() {
                        return Objects.hash(link, originallink);
                }
        }
}
