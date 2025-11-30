# 구글 헤드라인 RSS 아카이브 백엔드 (Spring Boot + MariaDB)

이 프로젝트는 **구글 뉴스 헤드라인 RSS**를 5분마다 수집해 MariaDB 에 저장하고,
저장된 기사를 REST API와 jQuery 기반 화면으로 일자별 아카이브 형태로 보여주는 백엔드입니다.

## 1. 실행 전 준비 사항
- **JDK**: 17
- **빌드 도구**: Maven (프로젝트에 `mvnw` 포함)
- **DB**: MariaDB (또는 MySQL 호환), 10.x 이상 권장
- **DB 계정/권한**: RSS 데이터를 저장할 스키마와 계정을 직접 준비해야 합니다.
- **필수 설정 변경**: `src/main/resources/application.properties`
  - `spring.datasource.url` : `jdbc:mariadb://<host>:<port>/<schema>` 형식으로 입력
  - `spring.datasource.username` / `spring.datasource.password`
  - (포트 충돌 시) `server.port`

## 2. DB 테이블 구조 요약
`src/main/resources/schema.sql`을 MariaDB에서 실행하면 아래 테이블이 생성됩니다.

| 컬럼 | 설명 |
| --- | --- |
| ID | 자동 증가 PK |
| PUB_DATE | 기사 기준 시각 (KST) |
| PUB_DATE_RAW | RSS pubDate 원문 문자열 |
| ARTICLE_TITLE | `<li><a>` 텍스트 |
| ARTICLE_LINK | `<li><a href>` 값 |
| PRESS_NAME | `<li><font>` 텍스트 |
| CREATED_AT / UPDATED_AT | 행 생성/수정 시각 |

- **중복 방지**: `ARTICLE_LINK + PUB_DATE`에 UNIQUE 인덱스(`UX_HEADLINE_NEWS`)를 걸어 동일 기사 저장을 차단합니다.

## 3. 빌드 및 실행 방법
1. 소스를 받은 뒤 터미널에서 프로젝트 루트로 이동합니다.
2. (선택) DB에 `schema.sql` 내용을 실행해 `HEADLINE_NEWS` 테이블을 준비합니다.
3. `application.properties`에서 DB URL/계정 정보를 채웁니다.
4. 서버 실행
   - `./mvnw spring-boot:run`
   - 또는 패키징 후 실행: `./mvnw -DskipTests package` → `java -jar target/the198thstreet-0.0.1-SNAPSHOT.jar`
5. 콘솔 로그에서 `[구글 RSS 수집기]` 메시지가 5분마다 출력되는지 확인합니다.

## 4. 구글 RSS 수집 동작
- **스케줄 주기**: `news.collector.google.fixed-delay` (기본 300000ms, 즉 5분)
- **사용 URL**: `https://news.google.com/rss/topics/CAAqJggKIiBDQkFTRWdvSUwyMHZNRFZxYUdjU0FtdHZHZ0pMVWlnQVAB?hl=ko&gl=KR&ceid=KR%3Ako`
- **처리 순서**
  1. `GoogleHeadlineNewsService#fetchAndSaveGoogleHeadlineNews`가 @Scheduled 로 실행됩니다.
  2. RSS XML 을 HTTP GET 으로 받아옵니다.
  3. 각 `<item>`의 `<description>` HTML 을 언이스케이프 후 `<ol><li>` 목록을 찾아 기사 제목/링크/언론사를 추출합니다.
  4. pubDate 문자열을 Asia/Seoul 기준 `LocalDateTime`으로 변환합니다.
  5. `ARTICLE_LINK + PUB_DATE` 조합이 DB에 없을 때만 INSERT 합니다.
  6. 저장/스킵 건수를 로그에 남겨 상태를 쉽게 파악합니다.

## 5. 화면 진입 방법과 사용법
- **URL**: `http://localhost:8080/archive/headlines`
- **초기 화면**: 오늘 날짜 기준 기사 목록을 카드 형태로 보여줍니다.
- **날짜 이동**: 상단의 `◀ 어제`, `내일 ▶`, 날짜 입력 필드로 원하는 날짜를 선택하면 Ajax 로 목록이 즉시 갱신됩니다.
- **페이징**: 화면 하단 버튼(이전/다음)으로 페이지를 이동합니다.
- **기사 열기**: 카드 제목을 클릭하면 원문 링크가 새 탭에서 열립니다.
- **UI 컨셉**: "그날 무슨 일이?" 타이틀 아래, 하루치 헤드라인이 SNS 메시지 카드처럼 나열됩니다.

## 6. REST API 엔드포인트
- `GET /api/archive/headlines`
  - 파라미터: `date`(yyyy-MM-dd, 기본 오늘), `page`(0 기반, 기본 0), `size`(기본 50)
  - 응답 예시
    ```json
    {
      "date": "2025-11-30",
      "totalCount": 123,
      "page": 0,
      "size": 50,
      "articles": [
        {
          "pubDate": "2025-11-30 08:44:00",
          "pressName": "경향신문",
          "articleTitle": "잇단 노동자 사망 사고에…",
          "articleLink": "https://news.google.com/..."
        }
      ]
    }
    ```
- **화면 호출 순서**: 페이지 로딩 → jQuery `$.getJSON('/api/archive/headlines', {date, page, size})` 호출 → 응답을 카드로 렌더링 → 날짜/페이지 버튼 클릭 시 동일 API 재호출.

## 7. 자주 발생할 수 있는 문제와 해결 방법
- **DB 접속 오류**: `spring.datasource.*` 값이 올바른지, MariaDB 가 실행 중인지, 계정 권한이 있는지 확인합니다.
- **RSS 호출 실패**: 콘솔에 `[구글 RSS 수집기 오류]` 로그가 남습니다. 네트워크/프록시 환경을 먼저 점검하세요.
- **화면에 데이터 없음**: 선택한 날짜에 저장된 뉴스가 없을 수 있습니다. 스케줄러가 정상 동작 중인지 로그로 확인하고, DB 테이블에 데이터가 들어왔는지 조회합니다.
- **시간대가 어색하게 보임**: pubDate 는 Asia/Seoul 기준 `yyyy-MM-dd HH:mm:ss` 문자열로 응답합니다. DB 저장 시점과 비교해 확인해 보세요.

## 8. 코드 흐름 빠르게 훑어보기
- `GoogleHeadlineNewsService` : RSS 호출/파싱/중복 확인/DB 저장의 핵심 로직
- `HeadlineNewsRepository` : JdbcTemplate 으로 Map 기반 INSERT & 날짜별 조회
- `ArchiveHeadlineApiController` : `/api/archive/headlines` JSON 응답
- `ArchiveHeadlineViewController` + `templates/archive/headlines.html` : 확장자 없는 URL을 제공하고, jQuery 로 API 결과를 카드 UI로 그립니다.

