package com.the198thstreet.news.repository;

import com.the198thstreet.news.entity.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsByOriginallink(String originallink);

    boolean existsByTitle(String title);
}
