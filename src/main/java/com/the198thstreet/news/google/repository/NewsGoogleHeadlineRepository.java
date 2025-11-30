package com.the198thstreet.news.google.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.the198thstreet.news.google.entity.NewsGoogleHeadline;

public interface NewsGoogleHeadlineRepository extends JpaRepository<NewsGoogleHeadline, Long> {

    boolean existsByArticleUrl(String articleUrl);

    @Query("""
            SELECT h FROM NewsGoogleHeadline h
            WHERE (:fromDate IS NULL OR h.topicPubDate >= :fromDate)
              AND (:toDate IS NULL OR h.topicPubDate <= :toDate)
              AND (:press IS NULL OR LOWER(h.articlePressName) LIKE LOWER(CONCAT('%', :press, '%')))
              AND (:keyword IS NULL OR LOWER(h.articleTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<NewsGoogleHeadline> search(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("press") String press,
            @Param("keyword") String keyword,
            Pageable pageable);
}
