package com.the198thstreet.news.repository;

import com.the198thstreet.news.entity.NewsItem;
import com.the198thstreet.news.entity.NewsSearchResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 뉴스 검색 결과와 기사 정보를 직접 SQL로 다루는 MyBatis 매퍼.
 * XML 매퍼 파일과 1:1로 연결되어 복잡한 레이어 없이 바로 DB 접근합니다.
 */
@Mapper
public interface NewsMapper {

    // 검색 결과 헤더 및 기사 입력
    void insertResult(NewsSearchResult result);

    void insertItems(List<NewsItem> items);

    // 조회
    List<NewsSearchResult> findAllResultsWithItems();

    NewsSearchResult findResultWithItems(Long id);

    NewsSearchResult findResultById(Long id);

    NewsItem findItemById(Long id);

    // 업데이트
    void updateResult(NewsSearchResult result);

    void updateItem(NewsItem item);

    // 삭제
    void deleteItemsByResultId(Long resultId);

    void deleteResult(Long resultId);

    void deleteItem(Long itemId);

    // 중복 체크
    boolean existsByOriginallink(String originallink);

    boolean existsByTitle(String title);
}
