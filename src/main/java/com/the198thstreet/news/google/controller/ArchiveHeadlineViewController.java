package com.the198thstreet.news.google.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 뉴스 아카이브 화면으로 이동시키는 단순 뷰 컨트롤러.
 * 확장자(.html)를 URL 에 노출하지 않고, /archive/headlines 경로로 접근한다.
 */
@Controller
public class ArchiveHeadlineViewController {

    @GetMapping("/archive/headlines")
    public String archivePage() {
        return "archive/headlines";
    }
}
