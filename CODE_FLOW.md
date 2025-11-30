# 구글 뉴스 수집·조회 전체 흐름 (최초 실행부터 화면 확인까지)

이 문서는 **엔티티/복잡한 계층 없이 Map + SQL만으로** 돌아가는 현재 코드의 전 과정을 아주 자세히 설명합니다.
초초급 개발자도 그대로 따라 할 수 있도록 "왜 이렇게 했는지" 이유까지 적었습니다.

## 1. 실행 준비
1. `src/main/resources/application.properties`에서 DB 연결 정보와 RSS URL을 확인합니다.
   - `news.collector.google.enabled=true` 이면 스케줄러가 자동으로 돌기 시작합니다.
   - `news.collector.google.fixed-delay=300000` 은 5분(300,000ms)마다 돌겠다는 뜻입니다.
2. MariaDB에 `schema.sql`이 자동 실행되어 `NEWS_GOOGLE_HEADLINE` 테이블이 생성됩니다.
3. `pom.xml`은 웹/스케줄러/Thymeleaf/JdbcTemplate만 포함합니다. JPA, 엔티티 관련 의존성은 없습니다.

## 2. 애플리케이션 기동
- `The198thstreetApplication`의 `main`을 실행하면 스프링 부트가 뜨고, `@EnableScheduling` 덕분에
  `GoogleHeadlineScheduler`가 스케줄 테이블에 등록됩니다.
- 스케줄러는 `fixedDelay` 설정에 따라 반복 실행되며, 실행 시점마다 로그(`구글 헤드라인 수집 스케줄 실행`)가 찍힙니다.

## 3. 수집 단계 세부 흐름
1. `GoogleHeadlineScheduler.run()` → `GoogleHeadlineService.collectHeadlines()` 호출.
2. `GoogleHeadlineService.collectHeadlines()` 내부 단계
   1. `news.collector.google.enabled`가 `false`이면 바로 리턴하여 불필요한 호출을 막습니다.
   2. `GoogleHeadlineRssClient.fetchRss()`로 RSS URL을 GET 호출합니다.
      - HTTP 200이 아니거나 예외가 나면 `null`을 반환하고 경고 로그만 남깁니다.
   3. 응답 XML 문자열을 `GoogleHeadlineParser.parse()`에 넘깁니다.
      - DOM 파서에서 `item` 노드를 순회하고, `description`의 HTML을 Jsoup으로 읽어 `li`마다 **Map**을 만듭니다.
      - Map 키: `topicGuid`, `topicTitle`, `topicLink`, `topicSourceName`, `topicSourceUrl`, `topicPubDate(Timestamp)`,
        `articleOrder`, `articleTitle`, `articleUrl`, `articlePressName`.
      - DTO/엔티티가 없으므로 키 이름만 알면 어디서든 동일하게 접근할 수 있습니다.
   4. 각 Map에 대해 중복 체크 후 DB INSERT
      - `isDuplicate()`에서 `ARTICLE_URL` 기준 COUNT(*)를 조회합니다.
      - 중복이면 `skippedCount` 증가 후 건너뜁니다.
      - 신규이면 `insertArticle()`에서 단순 INSERT SQL을 실행하며 `CREATED_AT`, `UPDATED_AT`에 현재 시간을 넣습니다.
   5. 전체 건수, 신규/중복 건수를 INFO 로그로 출력합니다.

## 4. 조회(API + 화면) 흐름
1. **REST API**: `GET /api/news/google/headlines`
   - 컨트롤러(`GoogleHeadlineApiController`)는 서비스의 `search()`가 돌려주는 Map을 그대로 응답합니다.
   - 응답 구조 예시
     ```json
     {
       "content": [ {"ARTICLE_TITLE": "...", "ARTICLE_URL": "...", ...} ],
       "page": 0,
       "size": 20,
       "totalElements": 120,
       "totalPages": 6,
       "hasPrevious": false,
       "hasNext": true
     }
     ```
2. **화면(Thymeleaf)**: `GET /news/google/headlines`
   - 똑같은 `search()` 결과 Map을 모델에 실어 템플릿에서 바로 사용합니다.
   - `headlines`는 `List<Map>` 그대로이며, `headline.TOPIC_PUB_DATE` 같은 키로 값을 꺼냅니다.
   - 페이지 이동 링크는 `hasPrevious/hasNext` 값을 그대로 활용합니다.

## 5. search() 내부 로직 상세
1. 파라미터 기본값 적용: page 0, size 20.
2. `WHERE` 절 조립
   - 날짜는 UTC 기준 00:00:00 ~ 23:59:59 로 변환 후 `TOPIC_PUB_DATE` 비교.
   - `press`, `keyword`는 LOWER + LIKE 로 부분 검색.
3. 총 건수(`COUNT(*)`)를 먼저 구해 `totalPages` 계산.
4. 데이터 조회 SQL
   ```sql
   SELECT ID, TOPIC_TITLE, TOPIC_PUB_DATE, ARTICLE_TITLE, ARTICLE_URL, ARTICLE_PRESS_NAME
   FROM NEWS_GOOGLE_HEADLINE
   ...WHERE 조건...
   ORDER BY TOPIC_PUB_DATE DESC
   LIMIT ? OFFSET ?
   ```
5. 조회 결과는 `JdbcTemplate.queryForList`로 받아 그대로 `content`에 담습니다.

## 6. 이유 정리 (왜 이렇게 단순화했나요?)
- **엔티티/DTO 제거**: Map 하나로 수집·저장·응답을 모두 처리하여 클래스 수를 최소화했습니다.
- **SQL 가시성**: 모든 쿼리를 문자열로 직접 작성해 데이터가 어떻게 저장/조회되는지 눈에 보이게 했습니다.
- **방어적 로깅**: 실패 지점을 모두 로그로 남겨 문제를 추적하기 쉽게 했습니다.
- **초보자 친화**: 흐름이 "RSS 호출 → XML 파싱 → Map 만들기 → DB INSERT → Map 그대로 응답" 으로 단선적입니다.

## 7. 직접 실행해보기
1. MariaDB 가동 후 `application.properties`의 접속 정보를 맞춥니다.
2. `./mvnw spring-boot:run` 실행.
3. 애플리케이션이 뜨면 5분마다 구글 RSS를 읽어 DB에 쌓입니다.
4. 브라우저에서 `http://localhost:8080/news/google/headlines` 접속 → 필터 입력 → 검색.
5. 동일 데이터를 JSON으로 보고 싶다면 `http://localhost:8080/api/news/google/headlines` 호출.

## 8. 로그로 확인하는 포인트
- `구글 헤드라인 수집 스케줄 실행` : 스케줄러 진입 여부
- `구글 RSS 호출 실패` : 네트워크/HTTP 오류
- `아이템 파싱 중 오류 발생` : 특정 item 의 HTML 구조가 예상과 다를 때
- `구글 헤드라인 수집 완료 - 전체:x건, 신규:y건, 중복:z건` : 실행 결과 요약
