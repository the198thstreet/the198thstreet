package com.the198thstreet.news.google.parser;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

@Component
public class GoogleHeadlineParser {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineParser.class);
    private static final DateTimeFormatter PUB_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    public List<GoogleHeadlineArticle> parse(String xml) {
        List<GoogleHeadlineArticle> articles = new ArrayList<>();
        if (!StringUtils.hasText(xml)) {
            return articles;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            org.w3c.dom.Document document = builder.parse(inputSource);
            NodeList itemNodes = document.getElementsByTagName("item");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node item = itemNodes.item(i);
                try {
                    parseItem(item, articles);
                } catch (Exception itemException) {
                    log.warn("아이템 파싱 중 오류 발생: {}", itemException.getMessage());
                }
            }
        } catch (Exception parseException) {
            log.warn("RSS 파싱 중 오류 발생: {}", parseException.getMessage());
        }

        return articles;
    }

    private void parseItem(Node item, List<GoogleHeadlineArticle> articles) {
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
                continue;
            }
            GoogleHeadlineArticle article = new GoogleHeadlineArticle();
            article.setTopicGuid(guid);
            article.setTopicTitle(title);
            article.setTopicLink(link);
            article.setTopicSourceName(sourceName);
            article.setTopicSourceUrl(sourceUrl);
            article.setTopicPubDate(topicPubDate);
            article.setArticleOrder(order.getAndIncrement());
            article.setArticleTitle(anchor.text());
            article.setArticleUrl(anchor.attr("href"));
            Element press = li.selectFirst("font");
            if (press != null) {
                article.setArticlePressName(press.text());
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
