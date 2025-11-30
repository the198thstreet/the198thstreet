package com.the198thstreet.news.google.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.the198thstreet.news.google.service.GoogleHeadlineService;

/**
 * 저장된 헤드라인을 JSON 형태로 제공하는 단순 REST 컨트롤러입니다.
 * <p>리턴 타입을 Map 으로 고정해 직관적으로 응답 구조를 확인할 수 있습니다.</p>
 */
@RestController
@RequestMapping("/api/news/google/headlines")
public class GoogleHeadlineApiController {

    private final GoogleHeadlineService service;

    public GoogleHeadlineApiController(GoogleHeadlineService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> getHeadlines(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate,
            @RequestParam(name = "press", required = false) String press,
            @RequestParam(name = "keyword", required = false) String keyword) {
        // 서비스는 Map 을 그대로 반환하므로 별도의 변환 단계가 없습니다.
        return service.search(page, size, parseDate(fromDate), parseDate(toDate), press, keyword);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            // 잘못된 날짜 문자열은 조용히 무시하고 null 로 처리해 필터를 적용하지 않습니다.
            return null;
        }
    }
}
