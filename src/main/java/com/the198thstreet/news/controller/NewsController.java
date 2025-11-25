package com.the198thstreet.news.controller;

import com.the198thstreet.news.model.NewsSearchResponse;
import com.the198thstreet.news.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 뉴스 검색 API 엔드포인트를 노출하는 REST 컨트롤러.
 * <p>
 * 입력값 검증 후 서비스 계층에 위임하며, 별도의 DTO 없이 {@link NewsSearchResponse} 레코드를 그대로 반환한다.
 */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * 검색어와 정렬 조건에 따라 뉴스 목록을 반환한다.
     *
     * @param query   검색어 (필수)
     * @param sort    정렬 방식(sim: 유사도, date: 날짜). 기본값은 sim
     * @param display 한 번에 받을 건수(1~100). 미입력 시 네이버 기본값 10을 따름
     * @param start   몇 번째 결과부터 가져올지(1~1000). 미입력 시 1부터 시작
     * @return 뉴스 검색 결과 레코드
     */
    @GetMapping
    public NewsSearchResponse listNews(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "sort", defaultValue = "sim") String sort,
            @RequestParam(name = "display", required = false) Integer display,
            @RequestParam(name = "start", required = false) Integer start) {

        // 빈 검색어에 대해서는 즉시 400 에러를 반환해 불필요한 외부 호출을 방지한다.
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력해 주세요.");
        }

        // 네이버 API 스펙에 맞춰 display(1~100), start(1~1000) 범위를 선검증한다.
        if (display != null && (display < 1 || display > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "display 값은 1~100 사이여야 합니다.");
        }
        if (start != null && (start < 1 || start > 1000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start 값은 1~1000 사이여야 합니다.");
        }

        return newsService.fetchNews(query, sort, display, start);
    }
}
