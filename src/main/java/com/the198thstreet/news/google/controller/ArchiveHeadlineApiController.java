package com.the198thstreet.news.google.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.the198thstreet.news.google.repository.HeadlineNewsRepository;

/**
 * 구글 헤드라인 아카이브 조회를 위한 REST API 컨트롤러.
 * <p>
 * /api/archive/headlines?date=yyyy-MM-dd&page=0&size=50 형태로 호출하면
 * 지정한 날짜의 기사 목록을 Map 기반 JSON 으로 돌려준다.
 */
@RestController
@RequestMapping("/api/archive/headlines")
public class ArchiveHeadlineApiController {

    private static final Logger log = LoggerFactory.getLogger(ArchiveHeadlineApiController.class);
    private static final DateTimeFormatter DATE_PARAM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HeadlineNewsRepository repository;

    public ArchiveHeadlineApiController(HeadlineNewsRepository repository) {
        this.repository = repository;
    }

    /**
     * 날짜별 헤드라인 목록을 페이징 조회한다.
     * 날짜 파라미터가 없으면 오늘(Asia/Seoul 기준)을 사용한다.
     */
    @GetMapping
    public Map<String, Object> getHeadlines(
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size) {

        LocalDate targetDate = resolveDate(date);
        int offset = Math.max(page, 0) * Math.max(size, 1);
        int totalCount = repository.countByDate(targetDate);
        List<Map<String, Object>> articles = repository.findByDate(targetDate, offset, Math.max(size, 1));

        // 날짜/시간 포맷팅을 Map 에서 바로 처리해 JSON 이 보기 좋도록 만든다.
        articles.forEach(article -> {
            LocalDateTime pubDate = (LocalDateTime) article.get("pubDate");
            article.put("pubDate", pubDate.format(DISPLAY_DATETIME));
        });

        Map<String, Object> response = new HashMap<>();
        response.put("date", targetDate.format(DATE_PARAM_FORMATTER));
        response.put("totalCount", totalCount);
        response.put("page", Math.max(page, 0));
        response.put("size", Math.max(size, 1));
        response.put("articles", articles);
        log.debug("[헤드라인 조회] date={} page={} size={} 조회건수={}", targetDate, page, size, articles.size());
        return response;
    }

    /**
     * 문자열 파라미터를 LocalDate 로 안전하게 변환한다. 실패 시 오늘 날짜를 돌려준다.
     */
    private LocalDate resolveDate(String dateParam) {
        if (dateParam == null || dateParam.isBlank()) {
            return LocalDate.now(KST);
        }
        try {
            return LocalDate.parse(dateParam, DATE_PARAM_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("[헤드라인 조회] 잘못된 날짜 형식이 전달되어 오늘 날짜로 대체합니다. 입력값={} 오류={}", dateParam, e.getMessage());
            return LocalDate.now(KST);
        }
    }
}
