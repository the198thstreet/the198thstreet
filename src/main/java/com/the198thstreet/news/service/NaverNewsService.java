package com.the198thstreet.news.service;

import com.the198thstreet.news.dto.NaverNewsItemResponse;
import com.the198thstreet.news.dto.NaverNewsResponse;
import com.the198thstreet.news.dto.NewsItemDto;
import com.the198thstreet.news.dto.NewsItemUpdateRequest;
import com.the198thstreet.news.dto.NewsSearchResultDto;
import com.the198thstreet.news.dto.NewsSearchResultUpdateRequest;
import com.the198thstreet.news.entity.NewsItem;
import com.the198thstreet.news.entity.NewsSearchResult;
import com.the198thstreet.news.repository.NewsItemRepository;
import com.the198thstreet.news.repository.NewsSearchResultRepository;
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
    private final NewsSearchResultRepository resultRepository;
    private final NewsItemRepository itemRepository;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.news-url}")
    private String newsApiUrl;

    @Transactional
    public NewsSearchResultDto fetchAndSave(String query, String sort) {
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
                .map(this::mapToEntity)
                .filter(this::isNewItem)
                .collect(Collectors.toList());

        items.forEach(result::addItem);
        NewsSearchResult saved = resultRepository.save(result);
        return NewsSearchResultDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<NewsSearchResultDto> findAllResults() {
        return resultRepository.findAll().stream()
                .map(NewsSearchResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<NewsSearchResultDto> findResult(Long id) {
        NewsSearchResult result = resultRepository.findWithItemsById(id);
        return Optional.ofNullable(result).map(NewsSearchResultDto::fromEntity);
    }

    @Transactional
    public Optional<NewsSearchResultDto> updateResult(Long id, NewsSearchResultUpdateRequest request) {
        return resultRepository.findById(id).map(result -> {
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
            return NewsSearchResultDto.fromEntity(result);
        });
    }

    @Transactional
    public void deleteResult(Long id) {
        resultRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<NewsItemDto> findItem(Long id) {
        return itemRepository.findById(id).map(NewsItemDto::fromEntity);
    }

    @Transactional
    public Optional<NewsItemDto> updateItem(Long id, NewsItemUpdateRequest request) {
        return itemRepository.findById(id).map(item -> {
            item.setTitle(request.getTitle());
            item.setOriginallink(request.getOriginallink());
            item.setLink(request.getLink());
            item.setDescription(request.getDescription());
            item.setPubDateRaw(request.getPubDateRaw());
            item.setPubDateDt(parseDate(request.getPubDateRaw()));
            return NewsItemDto.fromEntity(item);
        });
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    private NewsItem mapToEntity(NaverNewsItemResponse response) {
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
                item.getOriginallink() != null && itemRepository.existsByOriginallink(item.getOriginallink());
        boolean titleExists = item.getTitle() != null && itemRepository.existsByTitle(item.getTitle());
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
