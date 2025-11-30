package com.the198thstreet;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 메모리(Map)만 사용하는 초간단 뉴스 수집 스케줄러.
 * <p>
 * 네이버/구글 등 외부 API 호출이나 DB 저장소를 모두 제거하고,
 * "스케줄러가 돌 때마다 Map을 새로 채운다"는 동작만 남겼다.
 * 덕분에 네트워크나 데이터베이스 준비 없이도 애플리케이션을 켜서
 * 로그를 따라가며 흐름을 바로 확인할 수 있다.
 */
@Component
public class SimpleNewsScheduler {

        private static final Logger log = LoggerFactory.getLogger(SimpleNewsScheduler.class);

        /**
         * 사람이 읽기 쉬운 날짜/시간 포맷터.
         * 스케줄러가 언제 동작했는지 Map 안에 기록해 보여준다.
         */
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        /**
         * 스케줄러가 최근에 만든 기사 정보를 저장하는 Map.
         * key: 1, 2, 3 ... (순번)
         * value: 기사 정보를 담은 Map<String, String>
         * <p>
         * ConcurrentHashMap 을 사용해 스케줄러 스레드와 다른 스레드(예: 추후 REST API)가
         * 동시에 접근해도 안전하게 동작하도록 했다.
         */
        private final Map<Integer, Map<String, String>> inMemoryNews = new ConcurrentHashMap<>();

        /**
         * 스케줄러가 지금까지 몇 번 실행되었는지 세기 위한 카운터.
         * 실행 번호를 Map 값에 넣어, 로그만 보고도 "몇 번째 실행 데이터"인지 쉽게 알 수 있다.
         */
        private final AtomicInteger executionCounter = new AtomicInteger(0);

        /**
         * 스케줄러 켜짐/꺼짐 플래그. application.properties 에서 on/off 할 수 있다.
         * true 면 동작, false 면 로그만 남기고 바로 종료한다.
         */
        @Value("${news.collector.memory.enabled:true}")
        private boolean collectorEnabled;

        /**
         * 스케줄 주기(밀리초). 문자열로 읽어와 @Scheduled 의 fixedRateString 에 그대로 사용한다.
         * 값을 바꾸면 애플리케이션 재시작 없이도 다음 실행부터 새로운 주기가 반영된다.
         */
        @Value("${news.collector.memory.fixed-rate-ms:60000}")
        private String fixedRateMillis;

        /**
         * 1) collectorEnabled 가 true 일 때만 동작한다.
         * 2) 실행할 때마다 inMemoryNews 를 새로 채운다.
         * 3) 외부 시스템이 없으므로 실패 요소가 없고, 로그만 보면 전체 흐름을 파악할 수 있다.
         */
        @Scheduled(fixedRateString = "${news.collector.memory.fixed-rate-ms:60000}")
        public void refreshNewsCache() {
                if (!collectorEnabled) {
                        log.info("[메모리 뉴스 스케줄러] 비활성화 상태라서 실행하지 않습니다.");
                        return;
                }

                Instant startedAt = Instant.now();
                int executionNumber = executionCounter.incrementAndGet();

                log.info("[메모리 뉴스 스케줄러 시작] 실행번호={} 주기={}ms", executionNumber, fixedRateMillis);

                // 예시 데이터를 만든 뒤 Map 에 순서대로 채워 넣는다.
                List<Map<String, String>> latestHeadlines = buildSampleHeadlines(executionNumber);
                inMemoryNews.clear();

                AtomicInteger articleOrder = new AtomicInteger(1);
                latestHeadlines.forEach(article -> {
                        int slot = articleOrder.getAndIncrement();
                        inMemoryNews.put(slot, article);
                        log.debug("[기사 저장] 순번={} 내용={} ", slot, article);
                });

                long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
                log.info("[메모리 뉴스 스케줄러 완료] 실행번호={} 저장된 건수={} 소요시간={}ms", executionNumber, inMemoryNews.size(), elapsedMillis);
        }

        /**
         * 현재 스케줄러가 보관 중인 기사 Map 을 복사본으로 돌려준다.
         * <p>
         * Map 을 직접 반환하면 외부 코드가 내부 상태를 변경할 수 있으므로,
         * 새 LinkedHashMap 으로 한 번 감싸 안전하게 전달한다.
         * (초보자도 "불변 복사본" 개념을 쉽게 이해할 수 있도록 최대한 단순하게 표현했다.)
         */
        public Map<Integer, Map<String, String>> getCurrentNewsSnapshot() {
                return new LinkedHashMap<>(inMemoryNews);
        }

        /**
         * 보기 좋은 예시 기사 3개를 Map 리스트로 만들어 준다.
         * <p>
         * - title       : 기사 제목
         * - summary     : 한 줄 요약
         * - source      : 제공처 (샘플이라서 "LOCAL SAMPLE" 값 사용)
         * - collectedAt : 스케줄러가 데이터를 만든 시각
         * - executionNo : 몇 번째 스케줄러 실행에서 나온 데이터인지 표시
         */
        private List<Map<String, String>> buildSampleHeadlines(int executionNumber) {
                LocalDateTime now = LocalDateTime.now();
                return List.of(
                                createArticleMap("상태 점검 리포트", "외부 API 없이 메모리만 사용하여 안전하게 동작합니다.", "LOCAL SAMPLE", now, executionNumber),
                                createArticleMap("코드 읽기 가이드", "Map 구조만 사용하므로 데이터 흐름을 쉽게 따라갈 수 있습니다.", "LOCAL SAMPLE", now.plusSeconds(5), executionNumber),
                                createArticleMap("학습용 알림", "설정 값을 바꾸면 다음 실행부터 바로 반영됩니다.", "LOCAL SAMPLE", now.plusSeconds(10), executionNumber)
                );
        }

        /**
         * 개별 기사 Map 을 만들어 준다.
         * LinkedHashMap 을 사용해 항목이 입력한 순서대로 유지되도록 했다.
         */
        private Map<String, String> createArticleMap(String title, String summary, String source, LocalDateTime collectedAt, int executionNumber) {
                Map<String, String> article = new LinkedHashMap<>();
                article.put("title", title);
                article.put("summary", summary);
                article.put("source", source);
                article.put("collectedAt", collectedAt.format(DISPLAY_FORMATTER));
                article.put("executionNo", String.valueOf(executionNumber));
                return article;
        }
}
