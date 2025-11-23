-- 문자셋은 한글 고려해서 utf8mb4 기준
CREATE TABLE news_search_result (
                                    result_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '뉴스 검색 결과 PK',
                                    last_build_raw   VARCHAR(50)     NOT NULL COMMENT 'lastBuildDate 원본 문자열',
                                    last_build_dt    DATETIME        NULL     COMMENT 'lastBuildDate 파싱 값(옵션)',
                                    total_count      BIGINT          NOT NULL COMMENT '검색 결과 전체 건수 total',
                                    start_no         INT             NOT NULL COMMENT '검색 시작 위치 start',
                                    display_count    INT             NOT NULL COMMENT '한 번에 표시한 건수 display',
                                    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DB 저장일시',
                                    PRIMARY KEY (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='뉴스 검색 결과 헤더';
;

CREATE TABLE news_item (
                           news_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '뉴스 기사 PK',
                           result_id        BIGINT UNSIGNED NOT NULL COMMENT 'news_search_result FK',
                           title            VARCHAR(500)    NOT NULL COMMENT '기사 제목(HTML 태그 포함)',
                           originallink     VARCHAR(2048)   NULL     COMMENT '원문 링크',
                           link             VARCHAR(2048)   NOT NULL COMMENT '네이버 등 최종 링크',
                           description      TEXT            NULL     COMMENT '기사 요약/설명',
                           pub_date_raw     VARCHAR(50)     NOT NULL COMMENT 'pubDate 원본 문자열',
                           pub_date_dt      DATETIME        NULL     COMMENT 'pubDate 파싱 값(옵션)',
                           created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DB 저장일시',
                           PRIMARY KEY (news_id),
                           KEY idx_news_item_result_id (result_id),
                           KEY idx_news_item_pub_date_dt (pub_date_dt),
    -- 여기서 prefix index 사용
                           KEY idx_news_item_link (link(512)),
                           CONSTRAINT fk_news_item_result
                               FOREIGN KEY (result_id)
                                   REFERENCES news_search_result (result_id)
                                   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='뉴스 기사 목록';


-- 같은 링크 기사 중복 저장 방지용(선택)
ALTER TABLE news_item
    ADD UNIQUE KEY uq_news_item_link (link(512));
