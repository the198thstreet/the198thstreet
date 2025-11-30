package com.the198thstreet.news.google.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.the198thstreet.news.google.service.GoogleHeadlineService;
import com.the198thstreet.news.google.web.dto.NewsHeadlineDto;

@RestController
@RequestMapping("/api/news/google/headlines")
public class GoogleHeadlineApiController {

    private final GoogleHeadlineService service;

    public GoogleHeadlineApiController(GoogleHeadlineService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NewsHeadlineDto> getHeadlines(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate,
            @RequestParam(name = "press", required = false) String press,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return service.search(page, size, parseDate(fromDate), parseDate(toDate), press, keyword);
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
