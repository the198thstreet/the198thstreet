package com.the198thstreet.news.google.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 뉴스 아카이브 화면으로 이동시키는 단순 뷰 컨트롤러.
 * <p>
 * URL: {@code /archive/headlines}
 * 반환: templates/archive/headlines.html 템플릿 이름
 */
@Controller
public class ArchiveHeadlineViewController {

    @GetMapping("/archive/headlines")
    public String archivePage() {
        // 템플릿 파일 경로(src/main/resources/templates/archive/headlines.html)의 논리 이름을 반환한다.
        return "archive/headlines";
    }
}
