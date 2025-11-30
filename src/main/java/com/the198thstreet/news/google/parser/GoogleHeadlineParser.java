package com.the198thstreet.news.google.parser;

import java.io.StringReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 구글 뉴스 RSS XML과 description 내부의 HTML 조각을 아주 단순한 {@link Map} 리스트로 변환합니다.
 * <p>
 * <strong>왜 Map 으로 돌리나요?</strong><br>
 * DTO/엔티티 클래스를 만들어야 한다는 부담을 없애고, 키-값 쌍만으로 데이터를 옮길 수 있게 하기 위함입니다.
 * 초초급 개발자도 "필드 이름 = 키"만 기억하면 되므로 디버깅이 쉽습니다.
 */
@Component
public class GoogleHeadlineParser {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineParser.class);
    private static final DateTimeFormatter PUB_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    /**
     * RSS XML 전체를 받아 기사(Map) 목록을 반환합니다.
     * @param xml 구글 RSS 응답 문자열
     * @return 기사 정보를 담은 Map 리스트. 실패 시 빈 리스트.
     */
    public List<Map<String, Object>> parse(String xml) {
        List<Map<String, Object>> articles = new ArrayList<>();
        if (!StringUtils.hasText(xml)) {
            return articles;
        }

        try {
            // XXE 같은 보안 취약점을 막기 위해 안전 기능을 켠 상태로 DocumentBuilder를 만듭니다.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 문자열 기반 입력 소스를 DOM 객체로 파싱합니다.
            InputSource inputSource = new InputSource(new StringReader(xml));
            org.w3c.dom.Document document = builder.parse(inputSource);
            NodeList itemNodes = document.getElementsByTagName("item");

            // RSS의 각 item을 돌며 li 단위 기사 정보를 만들어 냅니다.
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node item = itemNodes.item(i);
                try {
                    parseItem(item, articles);
                } catch (Exception itemException) {
                    // 특정 item 만 실패해도 전체 수집을 멈추지 않도록 개별 예외를 잡습니다.
                    log.warn("아이템 파싱 중 오류 발생: {}", itemException.getMessage());
                }
            }
        } catch (Exception parseException) {
            log.warn("RSS 파싱 중 오류 발생: {}", parseException.getMessage());
        }

        return articles;
    }

    /**
     * 단일 item 노드를 받아 description 내부 li 태그를 모두 Map 으로 옮깁니다.
     * @param item RSS의 item 노드
     * @param articles 누적 결과 리스트
     */
    private void parseItem(Node item, List<Map<String, Object>> articles) {
        String guid = textContent(item, "guid");
        String title = textContent(item, "title");
        String link = textContent(item, "link");
        String sourceName = textContent(item, "source");
        String sourceUrl = attribute(item, "source", "url");
        String description = textContent(item, "description");
        String pubDate = textContent(item, "pubDate");

        LocalDateTime topicPubDate = parsePubDate(pubDate);
        String unescapedDescription = StringEscapeUtils.unescapeHtml4(description);
        if (!StringUtils.hasText(unescapedDescription)) {
            return;
        }

        Document descriptionDocument = Jsoup.parse(unescapedDescription);
        Elements listItems = descriptionDocument.select("li");
        AtomicInteger order = new AtomicInteger(1);

        for (Element li : listItems) {
            Element anchor = li.selectFirst("a[href]");
            if (anchor == null) {
                // 기사 링크가 없으면 저장 가치가 없는 항목이므로 건너뜁니다.
                continue;
            }

            Map<String, Object> article = new HashMap<>();
            article.put("topicGuid", guid);
            article.put("topicTitle", title);
            article.put("topicLink", link);
            article.put("topicSourceName", sourceName);
            article.put("topicSourceUrl", sourceUrl);
            article.put("topicPubDate", Timestamp.valueOf(topicPubDate)); // DB에 바로 넣기 쉬우려 Timestamp 사용
            article.put("articleOrder", order.getAndIncrement());
            article.put("articleTitle", anchor.text());
            article.put("articleUrl", anchor.attr("href"));

            Element press = li.selectFirst("font");
            if (press != null) {
                article.put("articlePressName", press.text());
            }
            articles.add(article);
        }
    }

    private String textContent(Node node, String tagName) {
        NodeList nodeList = ((org.w3c.dom.Element) node).getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        Node child = nodeList.item(0).getFirstChild();
        return child != null ? child.getNodeValue() : null;
    }

    private String attribute(Node node, String tagName, String attributeName) {
        NodeList nodeList = ((org.w3c.dom.Element) node).getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        org.w3c.dom.Element element = (org.w3c.dom.Element) nodeList.item(0);
        return element.getAttribute(attributeName);
    }

    private LocalDateTime parsePubDate(String pubDate) {
        if (!StringUtils.hasText(pubDate)) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, PUB_DATE_FORMATTER);
            return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            log.warn("pubDate 파싱 실패 - {}", pubDate);
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
