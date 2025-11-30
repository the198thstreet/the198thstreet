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
 * description 내부의 li 태그들을 실제 기사 데이터로 간주하며,
 * ARTICLE_LINK + PUB_DATE 조합이 존재하면 저장을 건너뛴다.
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
     * news.collector.google.enabled=false 면 로그만 남기고 종료한다.
     * 주기는 news.collector.google.fixed-delay(ms)로 조정 가능하다.
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
            String xml = restTemplate.getForObject(rssUrl, String.class);
            List<Map<String, Object>> articles = parseRss(xml);
            log.info("[구글 RSS 수집기] 파싱된 기사 건수={} (insert 여부는 중복 검사 후 결정)", articles.size());

            int inserted = 0;
            int skipped = 0;
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
     * XML 문자열을 파싱하여 description 내부의 li 태그들을 기사 목록으로 변환한다.
     * <p>
     * 1) XML 을 Jsoup 의 xmlParser 로 읽어 item 리스트를 찾는다.
     * 2) description 의 HTML 엔티티를 unescape 한 뒤 Jsoup 으로 다시 파싱해 li 목록을 얻는다.
     * 3) 각 li 에서 a 태그 텍스트/링크, font 태그 텍스트(언론사)를 꺼내 Map 으로 묶는다.
     */
    public List<Map<String, Object>> parseRss(String xmlContent) {
        List<Map<String, Object>> parsedArticles = new ArrayList<>();
        if (!StringUtils.hasText(xmlContent)) {
            log.warn("[구글 RSS 파서] 전달된 XML 내용이 비어 있어 파싱을 건너뜁니다.");
            return parsedArticles;
        }

        Document xml = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());
        Elements items = xml.select("channel > item");
        for (Element item : items) {
            String pubDateRaw = item.selectFirst("pubDate") != null ? item.selectFirst("pubDate").text() : null;
            LocalDateTime pubDate = parsePubDate(pubDateRaw);
            if (pubDate == null) {
                continue; // 날짜가 없으면 중복 체크와 저장을 할 수 없으므로 스킵
            }

            String escapedDescription = item.selectFirst("description") != null
                    ? item.selectFirst("description").text()
                    : "";
            String decodedHtml = StringEscapeUtils.unescapeHtml4(escapedDescription);
            Document descriptionHtml = Jsoup.parse(decodedHtml);
            Elements listItems = descriptionHtml.select("ol > li");

            for (Element li : listItems) {
                Element anchor = li.selectFirst("a");
                Element press = li.selectFirst("font");
                if (anchor == null || press == null) {
                    log.debug("[구글 RSS 파서] anchor/font 태그를 찾을 수 없어 li 를 스킵합니다. li 내용={}", li.outerHtml());
                    continue;
                }

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
     * 파싱 실패 시 null 을 반환해 상위 로직에서 스킵하도록 유도한다.
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
