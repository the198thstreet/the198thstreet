# 코드 구성 및 동작 흐름

스케줄러 중심의 간단한 구조로, 복잡한 계층 없이 `RestTemplate` + `JdbcTemplate` 만 사용합니다. 로그를 자세히 남겨 **“지금 무엇을 하고 있는지”**를 눈으로 확인할 수 있게 했습니다.

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
  - 네이버 API 인증 키, 데이터소스, 로그 레벨을 외부화한 프로퍼티 파일입니다.

## 스케줄러가 자동 실행되는 원리

```mermaid
flowchart TD
    A[애플리케이션 시작 (main 메서드)] --> B[@SpringBootApplication 자동 설정]
    B --> C[@EnableScheduling]
    C --> D[ScheduledAnnotationBeanPostProcessor
스프링이 자동 등록하는 후처리기]
    D --> E[@Component 빈 스캔
NaverNewsScheduler 발견]
    E --> F[@Scheduled 메서드 등록
기본 TaskScheduler 사용]
    F --> G[주기적으로 callNaverNewsApi 실행]
```

- `@EnableScheduling`을 붙이면 스프링이 `ScheduledAnnotationBeanPostProcessor`를 자동 등록합니다.
- 이 후처리기가 모든 빈을 살펴 `@Scheduled`가 붙은 메서드를 찾아 **스케줄 테이블**에 등록합니다.
- 기본 `TaskScheduler`(스레드 풀 1개)가 주기적으로 메서드를 호출하므로, 애플리케이션을 켜기만 해도 스케줄러가 자동으로 동작합니다.

## 실행 흐름 (로그와 함께 따라가기)
1. 애플리케이션 기동 → `RestClientConfig`가 `RestTemplate`를 만들고, 스케줄러가 등록됩니다.
2. **1분마다** 스케줄러가 시작될 때 INFO 로그가 찍힙니다. (`[뉴스 스케줄러 시작] ...`)
3. 고정 파라미터(`query=속보`, `sort=sim`, `display=100`)와 `start` 값(`1, 11, …, 91` 순환)을 조합해 요청 URL을 만듭니다.
4. 응답 JSON을 DTO(`NaverNewsResponse`, `NewsItem`)로 매핑하고, 목록이 비어 있으면 WARN 로그만 남기고 종료합니다.
5. 각 기사에 대해 `(link OR originallink)`가 DB에 이미 있는지 `JdbcTemplate`으로 검사합니다.
   - 중복이면 DEBUG 로그: `중복 기사 스킵 - title: ...`
   - 신규이면 INSERT 후 DEBUG 로그: `신규 기사 저장 완료 - title: ...`
6. 한 번의 실행이 끝나면 INFO 로그로 저장 건수와 소요 시간을 출력합니다. (`[뉴스 스케줄러 완료] ...`)

## 로그 세팅
- `src/main/resources/application.properties`
  - `logging.level.com.the198thstreet=DEBUG` : 우리 코드의 상세 로그를 모두 출력
  - `logging.level.org.springframework.scheduling=INFO` : 스케줄러 동작 상태를 노출
  - `logging.pattern.console` : 시간·스레드·클래스를 포함한 콘솔 포맷

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
- `logging.*` 로 로그 레벨과 콘솔 포맷을 제어합니다.
