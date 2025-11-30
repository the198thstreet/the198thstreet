package com.the198thstreet.news.google.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.the198thstreet.news.google.GoogleNewsProperties;
import com.the198thstreet.news.google.client.GoogleHeadlineRssClient;
import com.the198thstreet.news.google.entity.NewsGoogleHeadline;
import com.the198thstreet.news.google.parser.GoogleHeadlineArticle;
import com.the198thstreet.news.google.parser.GoogleHeadlineParser;
import com.the198thstreet.news.google.repository.NewsGoogleHeadlineRepository;
import com.the198thstreet.news.google.web.dto.NewsHeadlineDto;

@Service
public class GoogleHeadlineService {

    private static final Logger log = LoggerFactory.getLogger(GoogleHeadlineService.class);

    private final GoogleNewsProperties properties;
    private final GoogleHeadlineRssClient rssClient;
    private final GoogleHeadlineParser parser;
    private final NewsGoogleHeadlineRepository repository;

    public GoogleHeadlineService(GoogleNewsProperties properties,
            GoogleHeadlineRssClient rssClient,
            GoogleHeadlineParser parser,
            NewsGoogleHeadlineRepository repository) {
        this.properties = properties;
        this.rssClient = rssClient;
        this.parser = parser;
        this.repository = repository;
    }

    @Transactional
    public void collectHeadlines() {
        if (!properties.isEnabled()) {
            log.debug("구글 뉴스 수집기가 비활성화되어 실행하지 않습니다.");
            return;
        }
        String rssUrl = properties.getRssUrl();
        if (!StringUtils.hasText(rssUrl)) {
            log.warn("구글 RSS URL 이 설정되지 않아 수집을 건너뜁니다.");
            return;
        }

        String xml = rssClient.fetchRss(rssUrl);
        if (!StringUtils.hasText(xml)) {
            log.warn("구글 RSS 응답이 비어있습니다.");
            return;
        }

        List<GoogleHeadlineArticle> articles = parser.parse(xml);
        AtomicInteger savedCount = new AtomicInteger();
        AtomicInteger skippedCount = new AtomicInteger();

        for (GoogleHeadlineArticle article : articles) {
            try {
                if (repository.existsByArticleUrl(article.getArticleUrl())) {
                    skippedCount.incrementAndGet();
                    continue;
                }
                NewsGoogleHeadline entity = toEntity(article);
                repository.save(entity);
                savedCount.incrementAndGet();
            } catch (Exception e) {
                log.warn("기사 저장 중 오류 발생 - url={} message={}", article.getArticleUrl(), e.getMessage());
            }
        }

        log.info("구글 헤드라인 수집 완료 - 전체:{}건, 신규:{}건, 중복:{}건", articles.size(), savedCount.get(), skippedCount.get());
    }

    public Page<NewsHeadlineDto> search(Integer page, Integer size, LocalDate fromDate, LocalDate toDate, String press, String keyword) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                Sort.by(Sort.Direction.DESC, "topicPubDate"));
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay().atOffset(ZoneOffset.UTC).toLocalDateTime() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toLocalDateTime() : null;

        Page<NewsGoogleHeadline> results = repository.search(from, to, normalize(press), normalize(keyword), pageable);
        return results.map(NewsHeadlineDto::fromEntity);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private NewsGoogleHeadline toEntity(GoogleHeadlineArticle article) {
        NewsGoogleHeadline entity = new NewsGoogleHeadline();
        entity.setTopicGuid(article.getTopicGuid());
        entity.setTopicTitle(article.getTopicTitle());
        entity.setTopicLink(article.getTopicLink());
        entity.setTopicSourceName(article.getTopicSourceName());
        entity.setTopicSourceUrl(article.getTopicSourceUrl());
        entity.setTopicPubDate(article.getTopicPubDate());
        entity.setArticleOrder(article.getArticleOrder());
        entity.setArticleTitle(article.getArticleTitle());
        entity.setArticleUrl(article.getArticleUrl());
        entity.setArticlePressName(article.getArticlePressName());
        return entity;
    }
}
