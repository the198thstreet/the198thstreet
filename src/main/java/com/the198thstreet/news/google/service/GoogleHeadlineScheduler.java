package com.the198thstreet.news.google.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoogleHeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineScheduler.class);
    private final GoogleHeadlineService service;

    public GoogleHeadlineScheduler(GoogleHeadlineService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${news.collector.google.fixed-delay:300000}")
    public void run() {
        log.debug("구글 헤드라인 수집 스케줄 실행");
        service.collectHeadlines();
    }
}
