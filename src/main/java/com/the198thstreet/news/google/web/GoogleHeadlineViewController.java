package com.the198thstreet.news.google.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.the198thstreet.news.google.service.GoogleHeadlineService;

/**
 * 간단한 HTML 테이블로 뉴스 아카이브를 보여주는 컨트롤러입니다.
 * <p>서비스에서 받은 Map 그대로를 모델에 실어 템플릿에서 바로 사용하도록 했습니다.</p>
 */
@Controller
public class GoogleHeadlineViewController {

    private final GoogleHeadlineService service;

    public GoogleHeadlineViewController(GoogleHeadlineService service) {
        this.service = service;
    }

    @GetMapping("/news/google/headlines")
    public String viewHeadlines(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate,
            @RequestParam(name = "press", required = false) String press,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {
        LocalDate from = parseDate(fromDate);
        LocalDate to = parseDate(toDate);

        Map<String, Object> headlines = service.search(page, size, from, to, press, keyword);

        // 화면에서 접근하기 쉽게 각각의 값들을 모델에 풀어서 올립니다.
        model.addAttribute("headlines", headlines.get("content"));
        model.addAttribute("page", headlines.get("page"));
        model.addAttribute("size", headlines.get("size"));
        model.addAttribute("totalElements", headlines.get("totalElements"));
        model.addAttribute("totalPages", headlines.get("totalPages"));
        model.addAttribute("hasPrevious", headlines.get("hasPrevious"));
        model.addAttribute("hasNext", headlines.get("hasNext"));

        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("press", press);
        model.addAttribute("keyword", keyword);
        return "news/google-headlines";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
