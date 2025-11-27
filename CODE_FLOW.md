# 코드 구성 및 동작 흐름

스케줄러 중심의 간단한 구조로, 복잡한 계층 없이 `RestTemplate` + `JdbcTemplate` 만 사용합니다.

## 주요 파일
- `src/main/java/com/the198thstreet/The198thstreetApplication.java`
  - 스케줄러를 활성화(`@EnableScheduling`)한 Spring Boot 진입점입니다.
- `src/main/java/com/the198thstreet/config/RestClientConfig.java`
  - UTF-8 문자열 컨버터를 우선 적용한 `RestTemplate` 빈을 제공합니다.
- `src/main/java/com/the198thstreet/NaverNewsScheduler.java`
  - 1분마다 네이버 뉴스 API를 호출하고, 받은 아이템을 DB에 적재하는 전체 로직이 들어 있습니다.
- `src/main/resources/schema.sql`
  - `news_articles` 테이블을 생성하는 DDL입니다.
- `src/main/resources/application.properties`
  - 네이버 API 인증 키와 데이터소스 설정을 외부화한 프로퍼티 파일입니다.

## 실행 흐름
1. 애플리케이션이 기동되면 `RestClientConfig`가 `RestTemplate` 빈을 등록하고, `@EnableScheduling` 덕분에 `NaverNewsScheduler`가 주기적으로 실행됩니다.
2. 스케줄러는 `START_POSITIONS`(1, 11, …, 91)을 순환하며 `start` 쿼리 파라미터를 정합니다.
3. 고정된 파라미터(`query=속보`, `sort=sim`, `display=100`)와 헤더(`X-Naver-Client-Id`, `X-Naver-Client-Secret`)를 붙여 네이버 뉴스 검색 API를 호출합니다.
4. 응답 JSON은 DTO(`NaverNewsResponse`, `NewsItem`)로 매핑되며, 목록이 비어 있으면 WARN 로그만 남기고 종료합니다.
5. 각 기사에 대해 `(link OR originallink)`가 기존 DB에 존재하는지 `JdbcTemplate`으로 확인하고, 중복이 없으면 `news_articles` 테이블에 INSERT 합니다. `pubDate` 파싱 실패 시에는 에러 로그를 남기고 NULL 로 저장합니다.
6. 스케줄이 1분마다 반복되며, `start` 값도 순차적으로 다시 순환합니다.

## DB 스키마 요약
`schema.sql`은 다음 열을 정의합니다.
- `id` (PK, AUTO_INCREMENT)
- `title` (VARCHAR 500)
- `originallink` / `link` (각각 VARCHAR 1000, UNIQUE)
- `description` (TEXT)
- `pub_date` (DATETIME)
- `reg_date` (DATETIME, 기본값 CURRENT_TIMESTAMP)

## 프로퍼티 키
- `naver.api.client-id`, `naver.api.client-secret`, `naver.api.news-url`
- `spring.datasource.*` 로 DB 연결 정보를 주입합니다.
