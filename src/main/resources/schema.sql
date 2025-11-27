-- 네이버 뉴스 검색 결과를 저장하기 위한 테이블 정의 (MySQL/MariaDB 호환)
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    originallink VARCHAR(1000),
    link VARCHAR(1000),
    description TEXT,
    pub_date DATETIME,
    reg_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_news_link UNIQUE (link),
    CONSTRAINT uk_news_originallink UNIQUE (originallink)
);
