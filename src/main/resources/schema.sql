-- 뉴스 데이터를 보관하기 위한 단순 테이블 정의
-- 제목(title)에 유니크 인덱스를 두어 중복 뉴스가 저장되지 않도록 한다.
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL UNIQUE,
    originallink VARCHAR(1000),
    link VARCHAR(1000),
    description VARCHAR(2000),
    pub_date VARCHAR(100),
    query VARCHAR(255) NOT NULL,
    sort VARCHAR(20) NOT NULL,
    start_pos INT NOT NULL,
    display_count INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
