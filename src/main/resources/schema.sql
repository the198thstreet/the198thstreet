-- 현재 버전에서는 외부 DB를 사용하지 않습니다.
-- SimpleNewsScheduler 가 메모리 Map 안에 샘플 기사를 담아두므로,
-- 스키마 정의가 필요 없다는 사실을 명시적으로 남겨둡니다.
-- (앞으로 DB로 확장할 때 이 파일을 다시 채우면 됩니다.)

-- ===========================================
-- 구글 헤드라인 RSS 아카이브용 테이블 정의 (MariaDB)
-- ===========================================
CREATE TABLE IF NOT EXISTS HEADLINE_NEWS (
    ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    PUB_DATE DATETIME NOT NULL,
    PUB_DATE_RAW VARCHAR(100),
    ARTICLE_TITLE VARCHAR(500) NOT NULL,
    ARTICLE_LINK VARCHAR(1000) NOT NULL,
    PRESS_NAME VARCHAR(200) NOT NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY UX_HEADLINE_NEWS (ARTICLE_LINK, PUB_DATE)
);
