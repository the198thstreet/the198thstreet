package com.the198thstreet.news.google.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.the198thstreet.news.google.GoogleNewsProperties;
import com.the198thstreet.news.google.client.GoogleHeadlineRssClient;
import com.the198thstreet.news.google.parser.GoogleHeadlineParser;

/**
 * 구글 헤드라인 수집과 DB 적재, 검색까지 한 곳에서 처리하는 서비스입니다.
 * <p>
 * - 데이터 구조는 모두 {@link Map} 으로 통일했습니다.<br>
 * - 중복 체크, INSERT, 조회 쿼리도 눈에 보이게 단순 SQL 로 작성했습니다.<br>
 * - "지금 뭘 하고 있는지"를 로그로 친절하게 남겨 디버깅 난이도를 낮췄습니다.
 */
@Service
public class GoogleHeadlineService {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineService.class);

    private final GoogleNewsProperties properties;
    private final GoogleHeadlineRssClient rssClient;
    private final GoogleHeadlineParser parser;
    private final JdbcTemplate jdbcTemplate;

    public GoogleHeadlineService(GoogleNewsProperties properties,
            GoogleHeadlineRssClient rssClient,
            GoogleHeadlineParser parser,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.rssClient = rssClient;
        this.parser = parser;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RSS를 호출 -> XML 파싱 -> 중복을 건너뛰며 DB INSERT 순서로 수행합니다.
     * <p>모든 단계가 try-catch 로 분리되어 있어 한 건 실패해도 다음 기사 처리는 계속 진행됩니다.</p>
     */
    public void collectHeadlines() {
        if (!properties.isEnabled()) {
            log.debug("구글 뉴스 수집기가 비활성화되어 실행하지 않습니다.");
            return;
        }
        String rssUrl = properties.getRssUrl();
        if (!StringUtils.hasText(rssUrl)) {
            log.warn("구글 RSS URL 이 설정되지 않아 수집을 건너뜁니다.");
            return;
        }

        // 1) RSS 호출
        String xml = rssClient.fetchRss(rssUrl);
        if (!StringUtils.hasText(xml)) {
            log.warn("구글 RSS 응답이 비어있습니다.");
            return;
        }

        // 2) XML -> Map 리스트 변환
        List<Map<String, Object>> articles = parser.parse(xml);
        AtomicInteger savedCount = new AtomicInteger();
        AtomicInteger skippedCount = new AtomicInteger();

        // 3) 각 기사에 대해 DB 저장 시도
        for (Map<String, Object> article : articles) {
            try {
                String articleUrl = (String) article.get("articleUrl");
                if (isDuplicate(articleUrl)) {
                    skippedCount.incrementAndGet();
                    continue;
                }
                insertArticle(article);
                savedCount.incrementAndGet();
            } catch (Exception e) {
                log.warn("기사 저장 중 오류 발생 - url={} message={}", article.get("articleUrl"), e.getMessage());
            }
        }

        log.info("구글 헤드라인 수집 완료 - 전체:{}건, 신규:{}건, 중복:{}건", articles.size(), savedCount.get(), skippedCount.get());
    }

    /**
     * 검색 필터를 적용해 단순한 Map 응답을 만듭니다.
     * @param page 0부터 시작하는 페이지 번호
     * @param size 페이지당 건수
     * @param fromDate 시작일(UTC 날짜)
     * @param toDate 종료일(UTC 날짜)
     * @param press 언론사 부분 검색어
     * @param keyword 기사 제목 키워드
     * @return content, totalElements, totalPages 등을 담은 Map
     */
    public Map<String, Object> search(Integer page, Integer size, LocalDate fromDate, LocalDate toDate, String press, String keyword) {
        int pageNumber = page != null ? Math.max(page, 0) : 0;
        int pageSize = size != null && size > 0 ? size : 20;

        // 1) WHERE 절과 파라미터를 동적으로 조립합니다.
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            // 하루의 시작(UTC)으로 변환
            LocalDateTime start = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC).toLocalDateTime();
            where.append(" AND TOPIC_PUB_DATE >= ?");
            params.add(Timestamp.valueOf(start));
        }
        if (toDate != null) {
            // 하루의 끝(23:59:59)으로 변환
            LocalDateTime end = toDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toLocalDateTime();
            where.append(" AND TOPIC_PUB_DATE <= ?");
            params.add(Timestamp.valueOf(end));
        }
        if (StringUtils.hasText(press)) {
            where.append(" AND LOWER(ARTICLE_PRESS_NAME) LIKE LOWER(?)");
            params.add("%" + press + "%");
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND LOWER(ARTICLE_TITLE) LIKE LOWER(?)");
            params.add("%" + keyword + "%");
        }

        // 2) 전체 건수를 먼저 구해 페이지 정보를 계산합니다.
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM NEWS_GOOGLE_HEADLINE" + where, Integer.class, params.toArray());
        int totalElements = total != null ? total : 0;
        int totalPages = (int) Math.ceil(totalElements / (double) pageSize);

        // 3) 실제 데이터 조회 (최근 발행일 기준 내림차순)
        String dataQuery = "SELECT ID, TOPIC_TITLE, TOPIC_PUB_DATE, ARTICLE_TITLE, ARTICLE_URL, ARTICLE_PRESS_NAME "
                + "FROM NEWS_GOOGLE_HEADLINE" + where + " ORDER BY TOPIC_PUB_DATE DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(pageNumber * pageSize);

        List<Map<String, Object>> content = jdbcTemplate.queryForList(dataQuery, params.toArray());

        // 4) 프런트/호출자가 바로 쓸 수 있게 Map 하나에 포장합니다.
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", pageNumber);
        response.put("size", pageSize);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("hasPrevious", pageNumber > 0);
        response.put("hasNext", pageNumber + 1 < totalPages);
        return response;
    }

    private boolean isDuplicate(String articleUrl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM NEWS_GOOGLE_HEADLINE WHERE ARTICLE_URL = ?",
                Integer.class,
                articleUrl);
        return count != null && count > 0;
    }

    private void insertArticle(Map<String, Object> article) {
        // created_at / updated_at 은 항상 현재 서버 시간을 넣습니다.
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.update(
                "INSERT INTO NEWS_GOOGLE_HEADLINE ("
                        + "TOPIC_GUID, TOPIC_TITLE, TOPIC_LINK, TOPIC_SOURCE_NAME, TOPIC_SOURCE_URL, TOPIC_PUB_DATE, "
                        + "ARTICLE_ORDER, ARTICLE_TITLE, ARTICLE_URL, ARTICLE_PRESS_NAME, CREATED_AT, UPDATED_AT"
                        + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                article.get("topicGuid"),
                article.get("topicTitle"),
                article.get("topicLink"),
                article.get("topicSourceName"),
                article.get("topicSourceUrl"),
                article.get("topicPubDate"),
                article.get("articleOrder"),
                article.get("articleTitle"),
                article.get("articleUrl"),
                article.get("articlePressName"),
                now,
                now);
    }
}
