package com.the198thstreet.news.google.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * HEADLINE_NEWS 테이블에 접근하기 위한 단순 JdbcTemplate 기반 저장소.
 * <p>
 * 복잡한 JPA 엔티티를 쓰지 않고, 모든 입출력은 {@code Map<String, Object>}로 주고받는다.
 * 기사 저장 전 중복을 확인하고, 날짜 범위 검색과 총 건수 조회도 모두 Map 기반으로 제공한다.
 */
@Repository
public class HeadlineNewsRepository {

    private static final Logger log = LoggerFactory.getLogger(HeadlineNewsRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public HeadlineNewsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * ARTICLE_LINK + PUB_DATE 조합이 이미 존재하는지 확인한다.
     * <p>
     * 입력: 기사 원문 링크, 기준 일시(LocalDateTime)
     * 반환: 이미 저장된 데이터가 있으면 true, 없으면 false
     */
    public boolean existsByLinkAndPubDate(String articleLink, LocalDateTime pubDate) {
        String sql = "SELECT COUNT(1) FROM HEADLINE_NEWS WHERE ARTICLE_LINK = ? AND PUB_DATE = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, articleLink, Timestamp.valueOf(pubDate));
        boolean exists = count != null && count > 0;
        if (exists) {
            log.debug("[중복 확인] 이미 저장된 기사 - link={}, pubDate={}", articleLink, pubDate);
        }
        return exists;
    }

    /**
     * 파싱된 기사 한 건을 HEADLINE_NEWS 테이블에 저장한다.
     * <p>
     * 기대하는 Map 키
     * - pubDate      : LocalDateTime (NOT NULL)
     * - pubDateRaw   : String (원문 pubDate)
     * - articleTitle : String
     * - articleLink  : String
     * - pressName    : String
     */
    public void insertHeadline(Map<String, Object> article) {
        String sql = "INSERT INTO HEADLINE_NEWS (PUB_DATE, PUB_DATE_RAW, ARTICLE_TITLE, ARTICLE_LINK, PRESS_NAME) "
                + "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                Timestamp.valueOf((LocalDateTime) article.get("pubDate")),
                article.get("pubDateRaw"),
                article.get("articleTitle"),
                article.get("articleLink"),
                article.get("pressName"));
    }

    /**
     * 지정한 날짜(00:00:00~23:59:59) 범위에 포함되는 총 기사 건수를 구한다.
     * <p>
     * LocalDate 를 받으면 내부에서 해당 일자의 시작~끝 시각을 계산해 BETWEEN 조건을 만든다.
     */
    public int countByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        String sql = "SELECT COUNT(1) FROM HEADLINE_NEWS WHERE PUB_DATE BETWEEN ? AND ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Timestamp.valueOf(start), Timestamp.valueOf(end));
        return count == null ? 0 : count;
    }

    /**
     * 지정한 날짜 범위의 기사를 페이징 조회한다.
     * <p>
     * 입력: 조회 기준 날짜, offset, size
     * 반환: pubDate/pressName/articleTitle/articleLink 키를 가진 Map 리스트
     */
    public List<Map<String, Object>> findByDate(LocalDate date, int offset, int size) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        String sql = "SELECT PUB_DATE, PRESS_NAME, ARTICLE_TITLE, ARTICLE_LINK "
                + "FROM HEADLINE_NEWS WHERE PUB_DATE BETWEEN ? AND ? "
                + "ORDER BY PUB_DATE DESC, ID DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new ArticleRowMapper(),
                Timestamp.valueOf(start), Timestamp.valueOf(end), size, offset);
    }

    /**
     * RowMapper 구현을 별도 클래스로 두어 SELECT 결과를 Map 으로 변환한다.
     * PUB_DATE 는 LocalDateTime 형태로 담아 이후 포맷팅을 쉽게 한다.
     */
    private static class ArticleRowMapper implements RowMapper<Map<String, Object>> {
        @Override
        public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put("pubDate", rs.getTimestamp("PUB_DATE").toLocalDateTime());
            row.put("pressName", rs.getString("PRESS_NAME"));
            row.put("articleTitle", rs.getString("ARTICLE_TITLE"));
            row.put("articleLink", rs.getString("ARTICLE_LINK"));
            return row;
        }
    }
}
