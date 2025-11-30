package com.the198thstreet.news.google.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일정 간격마다 {@link GoogleHeadlineService#collectHeadlines()}를 실행하는 스케줄러입니다.
 * <p>
 * - 고정 지연 값은 프로퍼티(news.collector.google.fixed-delay)에서 읽어와 바로 반영됩니다.<br>
 * - 서비스 내부에서 enabled 플래그를 확인하므로, 별도 조건문 없이 호출만 합니다.
 */
@Component
public class GoogleHeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineScheduler.class);
    private final GoogleHeadlineService service;

    public GoogleHeadlineScheduler(GoogleHeadlineService service) {
        this.service = service;
    }

    /**
     * 이전 실행이 끝난 뒤 설정된 지연 시간만큼 기다렸다가 다시 수행됩니다.
     * 로그 한 줄을 남겨 "지금 스케줄러가 돌고 있다"는 것을 눈으로 확인할 수 있게 했습니다.
     */
    @Scheduled(fixedDelayString = "${news.collector.google.fixed-delay:300000}")
    public void run() {
        log.debug("구글 헤드라인 수집 스케줄 실행");
        service.collectHeadlines();
    }
}
