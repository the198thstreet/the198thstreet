package com.the198thstreet.news.google.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.the198thstreet.news.google.GoogleNewsProperties;
import com.the198thstreet.news.google.repository.HeadlineNewsRepository;

/**
 * 구글 뉴스 헤드라인 RSS 를 주기적으로 수집하고 DB 에 적재하는 서비스.
 * <p>
 * 요구사항의 핵심 로직을 모두 이 클래스에 모아 두었으며,
 * description 내부의 {@code <li>} 태그를 "실제 기사"로 보고 Map 형태로 다룬다.
 * 데이터 중복을 방지하기 위해 ARTICLE_LINK + PUB_DATE 조합을 기준으로 insert 여부를 결정한다.
 */
@Service
public class GoogleHeadlineNewsService {

    /** 고정된 RSS URL (요구사항에 따라 변경 불가) */
    private static final String FIXED_RSS_URL = "https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRFZxYUdjU0FtdHZHZ0pMVWlnQVAB?hl=ko&gl=KR&ceid=KR%3Ako";

    /** pubDate 문자열 파싱용 RFC_1123 포맷터 */
    private static final DateTimeFormatter RFC1123_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("Asia/Seoul"));

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineNewsService.class);

    private final RestTemplate restTemplate;
    private final GoogleNewsProperties properties;
    private final HeadlineNewsRepository repository;

    public GoogleHeadlineNewsService(RestTemplate restTemplate, GoogleNewsProperties properties,
            HeadlineNewsRepository repository) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.repository = repository;
    }

    /**
     * 5분마다(기본값) RSS 를 가져와 DB 에 저장한다.
     * <p>
     * 동작 순서
     * 1) 스케줄이 실행되면 먼저 enabled 플래그를 확인한다.
     * 2) RSS URL 에 HTTP GET 요청을 보내 XML 을 문자열로 받는다.
     * 3) {@link #parseRss(String)} 로 XML 을 Map 리스트로 파싱한다.
     * 4) 각 기사에 대해 중복 여부를 확인하고, 없으면 {@link HeadlineNewsRepository#insertHeadline(Map)} 으로 저장한다.
     * 5) 저장/스킵 건수를 로그로 남겨 운영자가 흐름을 쉽게 파악하도록 돕는다.
     */
    @Scheduled(fixedDelayString = "${news.collector.google.fixed-delay:300000}")
    public void fetchAndSaveGoogleHeadlineNews() {
        if (!properties.isEnabled()) {
            log.info("[구글 RSS 수집기] 비활성화 설정으로 인해 실행을 건너뜁니다.");
            return;
        }

        try {
            String rssUrl = resolveRssUrl();
            log.info("[구글 RSS 수집기] URL={} 으로부터 헤드라인을 가져옵니다.", rssUrl);
            // 1. RSS XML 을 문자열로 받는다.
            String xml = restTemplate.getForObject(rssUrl, String.class);
            // 2. description 내부 li 태그만 뽑아 기사 목록으로 변환한다.
            List<Map<String, Object>> articles = parseRss(xml);
            log.info("[구글 RSS 수집기] 파싱된 기사 건수={} (insert 여부는 중복 검사 후 결정)", articles.size());

            int inserted = 0;
            int skipped = 0;
            // 3. 각 기사마다 중복 여부를 체크한 뒤 없으면 저장한다.
            for (Map<String, Object> article : articles) {
                if (repository.existsByLinkAndPubDate((String) article.get("articleLink"),
                        (LocalDateTime) article.get("pubDate"))) {
                    skipped++;
                    continue;
                }
                repository.insertHeadline(article);
                inserted++;
            }
            log.info("[구글 RSS 수집기 완료] 저장={}건, 중복 스킵={}건", inserted, skipped);
        } catch (RestClientException httpError) {
            log.error("[구글 RSS 수집기 오류] HTTP 호출 실패", httpError);
        } catch (Exception e) {
            log.error("[구글 RSS 수집기 오류] 예기치 못한 오류", e);
        }
    }

    /**
     * XML 문자열을 파싱하여 description 내부의 {@code <li>} 태그들을 기사 목록으로 변환한다.
     * <p>
     * 입력: 구글 뉴스 RSS XML 전체 문자열
     * 반환: 기사마다 아래 키를 담은 Map 리스트
     *  - articleTitle : {@code <a>} 태그 텍스트
     *  - articleLink  : {@code <a href>} 값
     *  - pressName    : {@code <font>} 텍스트
     *  - pubDateRaw   : 원문 pubDate 문자열
     *  - pubDate      : Asia/Seoul 기준 LocalDateTime
     */
    public List<Map<String, Object>> parseRss(String xmlContent) {
        List<Map<String, Object>> parsedArticles = new ArrayList<>();
        if (!StringUtils.hasText(xmlContent)) {
            log.warn("[구글 RSS 파서] 전달된 XML 내용이 비어 있어 파싱을 건너뜁니다.");
            return parsedArticles;
        }

        // 1. 전체 XML 을 Jsoup XML 파서로 읽어 item 목록을 찾는다.
        Document xml = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());
        Elements items = xml.select("channel > item");
        for (Element item : items) {
            // 2. pubDate 문자열을 그대로 담아두고, 변환 가능한지 확인한다.
            String pubDateRaw = item.selectFirst("pubDate") != null ? item.selectFirst("pubDate").text() : null;
            LocalDateTime pubDate = parsePubDate(pubDateRaw);
            if (pubDate == null) {
                continue; // 날짜가 없으면 중복 체크와 저장을 할 수 없으므로 스킵
            }

            // 3. description 은 HTML 이 이스케이프되어 있으므로 unescape → HTML 파싱 순서로 진행한다.
            String escapedDescription = item.selectFirst("description") != null
                    ? item.selectFirst("description").text()
                    : "";
            String decodedHtml = StringEscapeUtils.unescapeHtml4(escapedDescription);
            Document descriptionHtml = Jsoup.parse(decodedHtml);
            Elements listItems = descriptionHtml.select("ol > li");

            // 4. 각 li 태그를 돌며 기사 정보를 수집한다.
            for (Element li : listItems) {
                Element anchor = li.selectFirst("a");
                Element press = li.selectFirst("font");
                if (anchor == null || press == null) {
                    log.debug("[구글 RSS 파서] anchor/font 태그를 찾을 수 없어 li 를 스킵합니다. li 내용={}", li.outerHtml());
                    continue;
                }

                // 5. Map 구조에 기사 정보를 담아 리스트에 추가한다.
                Map<String, Object> article = new HashMap<>();
                article.put("articleTitle", anchor.text());
                article.put("articleLink", anchor.attr("href"));
                article.put("pressName", press.text());
                article.put("pubDateRaw", pubDateRaw);
                article.put("pubDate", pubDate);
                parsedArticles.add(article);
            }
        }
        return parsedArticles;
    }

    /**
     * pubDate 문자열을 RFC_1123 포맷으로 파싱해 LocalDateTime(Asia/Seoul) 으로 변환한다.
     * <p>
     * 입력: "Sun, 30 Nov 2025 08:44:00 GMT" 같은 pubDate 문자열
     * 반환: 한국 시간대(LocalDateTime) 또는 파싱 실패 시 null
     */
    private LocalDateTime parsePubDate(String pubDateRaw) {
        if (!StringUtils.hasText(pubDateRaw)) {
            log.warn("[구글 RSS 파서] pubDate 값이 비어 있어 변환할 수 없습니다.");
            return null;
        }
        try {
            return LocalDateTime.ofInstant(RFC1123_FORMATTER.parse(pubDateRaw, Instant::from), ZoneId.of("Asia/Seoul"));
        } catch (DateTimeParseException e) {
            log.error("[구글 RSS 파서] pubDate 파싱 실패. 원본={} 오류={}", pubDateRaw, e.getMessage());
            return null;
        }
    }

    /**
     * 프로퍼티에 rssUrl 이 지정되어 있으면 그것을 사용하고, 아니면 요구사항의 고정 URL 을 사용한다.
     * 사용자가 실수로 다른 값을 넣어도 기능 요구사항을 지키기 위해 고정 값 우선순위를 높였다.
     */
    private String resolveRssUrl() {
        return StringUtils.hasText(properties.getRssUrl()) ? properties.getRssUrl() : FIXED_RSS_URL;
    }
}
