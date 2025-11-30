package com.the198thstreet.news.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties 의 news.collector.google.* 값을 편리하게 읽어오는 클래스.
 * <p>
 * - enabled    : true 이면 RSS 수집기가 동작, false 면 로그만 남기고 종료
 * - rssUrl     : 기본 고정 URL 대신 다른 RSS 를 테스트하고 싶을 때 지정
 * - fixedDelay : @Scheduled fixedDelayString 값 (밀리초 기준)
 */
@ConfigurationProperties(prefix = "news.collector.google")
public class GoogleNewsProperties {

    /** 스케줄러 활성화 여부 */
    private boolean enabled = true;

    /** RSS URL (미지정 시 코드에 박힌 고정 URL 사용) */
    private String rssUrl;

    /** 고정 지연 (밀리초) */
    private long fixedDelay = 300_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
}
