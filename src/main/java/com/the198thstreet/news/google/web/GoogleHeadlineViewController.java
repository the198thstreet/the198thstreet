package com.the198thstreet.news.google.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.the198thstreet.news.google.service.GoogleHeadlineService;
import com.the198thstreet.news.google.web.dto.NewsHeadlineDto;

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
        Page<NewsHeadlineDto> headlines = service.search(page, size, from, to, press, keyword);

        model.addAttribute("headlines", headlines);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
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
