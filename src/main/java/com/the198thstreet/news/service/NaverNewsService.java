package com.the198thstreet.news.service;

import com.the198thstreet.news.dto.NaverNewsItemResponse;
import com.the198thstreet.news.dto.NaverNewsResponse;
import com.the198thstreet.news.dto.NewsItemDto;
import com.the198thstreet.news.dto.NewsItemUpdateRequest;
import com.the198thstreet.news.dto.NewsSearchResultDto;
import com.the198thstreet.news.dto.NewsSearchResultUpdateRequest;
import com.the198thstreet.news.entity.NewsItem;
import com.the198thstreet.news.entity.NewsSearchResult;
import com.the198thstreet.news.repository.NewsMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private static final Logger log = LoggerFactory.getLogger(NaverNewsService.class);
    private static final DateTimeFormatter NAVER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final NewsMapper newsMapper;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.news-url}")
    private String newsApiUrl;

    @Transactional
    public NewsSearchResultDto fetchAndSave(String query, String sort) {
        // 1) 쿼리 파라미터와 헤더를 깔끔하게 조립
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("query", query)
                .queryParam("sort", sort);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<NaverNewsResponse> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, httpEntity, NaverNewsResponse.class);

        NaverNewsResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("No response body from Naver API");
        }

        // Print to console as requested
        log.info("Received Naver news response: {}", body);
        System.out.println(body);

        NewsSearchResult result = new NewsSearchResult();
        result.setLastBuildRaw(body.getLastBuildDate());
        result.setLastBuildDt(parseDate(body.getLastBuildDate()));
        result.setTotalCount(body.getTotal());
        result.setStartNo(body.getStart());
        result.setDisplayCount(body.getDisplay());

        List<NewsItem> items = body.getItems().stream()
                .map(this::mapToModel)
                .filter(this::isNewItem)
                .collect(Collectors.toList());

        // 2) 헤더/본문을 단순하게 한 번씩만 insert
        result.setItems(items);
        newsMapper.insertResult(result);
        attachResultId(result);
        if (!items.isEmpty()) {
            newsMapper.insertItems(items);
        }
        return NewsSearchResultDto.fromModel(result);
    }

    @Transactional(readOnly = true)
    public List<NewsSearchResultDto> findAllResults() {
        return newsMapper.findAllResultsWithItems().stream()
                .map(NewsSearchResultDto::fromModel)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<NewsSearchResultDto> findResult(Long id) {
        NewsSearchResult result = newsMapper.findResultWithItems(id);
        return Optional.ofNullable(result).map(NewsSearchResultDto::fromModel);
    }

    @Transactional
    public Optional<NewsSearchResultDto> updateResult(Long id, NewsSearchResultUpdateRequest request) {
        return Optional.ofNullable(newsMapper.findResultById(id)).map(result -> {
            result.setLastBuildRaw(request.getLastBuildRaw());
            if (request.getLastBuildDt() != null) {
                result.setLastBuildDt(LocalDateTime.parse(request.getLastBuildDt()));
            }
            if (request.getTotalCount() != null) {
                result.setTotalCount(request.getTotalCount());
            }
            if (request.getStartNo() != null) {
                result.setStartNo(request.getStartNo());
            }
            if (request.getDisplayCount() != null) {
                result.setDisplayCount(request.getDisplayCount());
            }
            newsMapper.updateResult(result);
            return NewsSearchResultDto.fromModel(result);
        });
    }

    @Transactional
    public void deleteResult(Long id) {
        newsMapper.deleteItemsByResultId(id);
        newsMapper.deleteResult(id);
    }

    @Transactional(readOnly = true)
    public Optional<NewsItemDto> findItem(Long id) {
        return Optional.ofNullable(newsMapper.findItemById(id)).map(NewsItemDto::fromModel);
    }

    @Transactional
    public Optional<NewsItemDto> updateItem(Long id, NewsItemUpdateRequest request) {
        return Optional.ofNullable(newsMapper.findItemById(id)).map(item -> {
            item.setTitle(request.getTitle());
            item.setOriginallink(request.getOriginallink());
            item.setLink(request.getLink());
            item.setDescription(request.getDescription());
            item.setPubDateRaw(request.getPubDateRaw());
            item.setPubDateDt(parseDate(request.getPubDateRaw()));
            newsMapper.updateItem(item);
            return NewsItemDto.fromModel(item);
        });
    }

    @Transactional
    public void deleteItem(Long id) {
        newsMapper.deleteItem(id);
    }

    private void attachResultId(NewsSearchResult result) {
        if (result.getItems() == null) {
            return;
        }
        result.getItems().forEach(item -> item.setResultId(result.getId()));
    }

    private NewsItem mapToModel(NaverNewsItemResponse response) {
        NewsItem item = new NewsItem();
        item.setTitle(response.getTitle());
        item.setOriginallink(response.getOriginallink());
        item.setLink(response.getLink());
        item.setDescription(response.getDescription());
        item.setPubDateRaw(response.getPubDate());
        item.setPubDateDt(parseDate(response.getPubDate()));
        return item;
    }

    private boolean isNewItem(NewsItem item) {
        boolean originallinkExists =
                item.getOriginallink() != null && newsMapper.existsByOriginallink(item.getOriginallink());
        boolean titleExists = item.getTitle() != null && newsMapper.existsByTitle(item.getTitle());
        if (originallinkExists || titleExists) {
            log.info("Skipping duplicate news item with title='{}' and originallink='{}'",
                    item.getTitle(), item.getOriginallink());
            return false;
        }
        return true;
    }

    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(dateString, NAVER_DATE_FORMATTER).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Unable to parse date '{}': {}", dateString, e.getMessage());
            return null;
        }
    }
}
