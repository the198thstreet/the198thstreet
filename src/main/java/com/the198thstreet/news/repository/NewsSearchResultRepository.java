package com.the198thstreet.news.repository;

import com.the198thstreet.news.entity.NewsSearchResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSearchResultRepository extends JpaRepository<NewsSearchResult, Long> {

    @EntityGraph(attributePaths = "items")
    NewsSearchResult findWithItemsById(Long id);
}
