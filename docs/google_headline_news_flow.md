# 구글 뉴스 헤드라인 RSS 아카이브 흐름 상세 가이드

본 문서는 **구글 뉴스 헤드라인 RSS → 파싱 → DB 저장 → REST API → 화면** 흐름을 처음 접하는 초급 개발자도 이해할 수 있도록 아주 친절하게 설명합니다. 모든 코드 조각은 실제 프로젝트에 들어있는 클래스/설정과 1:1 로 대응합니다.

---
## 1) 전체 기능 개요
1. **RSS 수집**: `GoogleHeadlineNewsService#fetchAndSaveGoogleHeadlineNews`가 5분마다 고정된 RSS URL을 호출합니다.
2. **파싱**: 각 `<item>`의 `<description>` 내부에 있는 `<ol><li>` 목록을 Jsoup 으로 분석하여 기사 제목/링크/언론사를 꺼냅니다.
3. **DB 저장**: 파싱 결과를 `HeadlineNewsRepository`가 MariaDB 테이블 `HEADLINE_NEWS`에 저장합니다. (ARTICLE_LINK+PUB_DATE 조합이 이미 있으면 저장하지 않음)
4. **REST API 제공**: `/api/archive/headlines` 엔드포인트가 날짜별로 저장된 기사를 JSON 으로 반환합니다.
5. **화면 렌더링**: `/archive/headlines` 페이지가 jQuery 로 API를 호출하여 뉴스 카드 타임라인을 보여줍니다.

---
## 2) DB 테이블 설명 (HEADLINE_NEWS)
테이블 생성 DDL은 `src/main/resources/schema.sql`에 있습니다.

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| `ID` | 자동 증가 PK | 1 |
| `PUB_DATE` | 기사 기준 시각 (KST), pubDate 파싱 결과 | `2025-11-30 08:44:00` |
| `PUB_DATE_RAW` | RSS 원문 pubDate 문자열 | `Sun, 30 Nov 2025 08:44:00 GMT` |
| `ARTICLE_TITLE` | `<li><a>` 텍스트 | `잇단 노동자 사망 사고에…` |
| `ARTICLE_LINK` | `<li><a href>` | `https://news.google.com/...` |
| `PRESS_NAME` | `<li><font>` 텍스트 | `경향신문` |
| `CREATED_AT` | 행 생성 시각 | 자동 | 
| `UPDATED_AT` | 행 갱신 시각 | 자동 |

- **중복 방지 전략**: `ARTICLE_LINK + PUB_DATE`에 `UNIQUE INDEX`(`UX_HEADLINE_NEWS`)를 걸어 중복 저장을 차단합니다. 서비스 레이어에서도 insert 전에 `existsByLinkAndPubDate`로 다시 확인합니다.

---
## 3) RSS 파서 동작 설명
### 실제 item + description 예시
```xml
<item>
  <title>헤드라인 전체 제목</title>
  <link>...</link>
  <pubDate>Sun, 30 Nov 2025 08:44:00 GMT</pubDate>
  <description>
    &lt;ol&gt;
      &lt;li&gt;&lt;a href="https://news...1"&gt;기사 제목 1&lt;/a&gt;&nbsp;&nbsp;&lt;font&gt;언론사1&lt;/font&gt;&lt;/li&gt;
      &lt;li&gt;&lt;a href="https://news...2"&gt;기사 제목 2&lt;/a&gt;&nbsp;&nbsp;&lt;font&gt;언론사2&lt;/font&gt;&lt;/li&gt;
    &lt;/ol&gt;
  </description>
  <source>대표 언론사</source>
</item>
```

### 파싱 단계 (코드: `GoogleHeadlineNewsService#parseRss`)
1. XML 전체를 Jsoup 의 `xmlParser`로 읽어 `<channel><item>` 목록을 찾습니다.
2. 각 item의 `<pubDate>` 텍스트를 `RFC_1123_DATE_TIME` 포맷으로 파싱해 `LocalDateTime`(Asia/Seoul)으로 변환합니다. 변환 실패 시 해당 item 은 스킵합니다.
3. `<description>` 문자열을 `StringEscapeUtils.unescapeHtml4`로 HTML 엔티티를 디코딩합니다.
4. 디코딩된 HTML을 다시 Jsoup 로 파싱해 `<ol><li>`를 찾습니다.
5. 각 `<li>`에서
   - `articleTitle`: `<a>` 태그 텍스트
   - `articleLink`: `<a href>` 속성
   - `pressName`: `<font>` 텍스트
   를 추출하여 Map 으로 묶습니다. 같은 item 의 `pubDate`, `pubDateRaw`를 함께 넣습니다.

---
## 4) 배치 스케줄러 흐름
- 클래스/메서드: `GoogleHeadlineNewsService#fetchAndSaveGoogleHeadlineNews`
- 주기: `@Scheduled(cron = "0 0/5 * * * *")` → 애플리케이션 시작 후 5분마다 실행.
- 동작 순서:
  1. `news.collector.google.enabled` 값이 false 이면 로그만 남기고 종료.
  2. 고정 RSS URL(또는 프로퍼티에 지정된 URL)로 HTTP GET.
  3. 응답 XML 을 `parseRss`로 파싱 → 기사 Map 리스트 확보.
  4. 각 기사마다 `HeadlineNewsRepository.existsByLinkAndPubDate`로 중복 확인 후 insert.
  5. 저장 건수/스킵 건수를 로그로 남겨 운영자가 상태를 쉽게 확인할 수 있게 함.

---
## 5) REST API 설명: `/api/archive/headlines`
- Method: **GET**
- Query Params:
  - `date` (선택, `yyyy-MM-dd`, 기본값: 오늘 KST)
  - `page` (선택, 0 기반, 기본값: 0)
  - `size` (선택, 기본값: 50)
- 응답 JSON 예시
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
- 컨트롤러(`ArchiveHeadlineApiController`)와 저장소는 모두 `Map<String, Object>`/`List<Map<String, Object>>` 구조만 사용합니다.

---
## 6) 화면 동작 설명: `/archive/headlines`
1. 브라우저가 `/archive/headlines`로 이동하면 `archive/headlines.html`(Thymeleaf 템플릿) 이 렌더링됩니다. URL에 확장자는 노출되지 않습니다.
2. `document.ready` 시점에 오늘 날짜를 계산하여 `#selectedDate` 입력값을 채웁니다.
3. jQuery `$.getJSON('/api/archive/headlines', {date, page, size})` 호출로 초기 기사 목록을 불러옵니다.
4. 결과 JSON 의 `articles` 배열을 기반으로 SNS 카드 형태의 DOM 을 재구성합니다.
5. 날짜 이동(어제/내일 버튼, date input)이나 페이징 버튼 클릭 시 다시 Ajax 호출 → 카드 영역을 새로 그림.
6. 기사를 클릭하면 새 탭에서 원문 링크가 열립니다.

---
## 7) 실행 방법
1. **DB 준비**
   - MariaDB 에 접속해 `src/main/resources/schema.sql`의 `CREATE TABLE IF NOT EXISTS HEADLINE_NEWS ...` 구문을 실행합니다.
   - `ARTICLE_LINK + PUB_DATE` UNIQUE KEY 가 꼭 생성되어야 합니다.
2. **애플리케이션 설정** (`src/main/resources/application.properties`)
   - 기본값: `news.collector.google.enabled=true`, `news.collector.google.rss-url=<고정 RSS>`, `news.collector.google.fixed-delay=300000`.
   - MariaDB 접속 정보(`spring.datasource.url`, `username`, `password`)를 환경에 맞게 추가합니다. 기존 설정은 삭제하지 말고 필요한 값만 덧붙이세요.
3. **실행**
   - `./mvnw spring-boot:run` 실행.
   - 로그에서 `[구글 RSS 수집기]` 라인이 5분마다 출력되는지 확인합니다.
4. **브라우저 확인**
   - `http://localhost:8080/archive/headlines` 접속.
   - 오늘 날짜가 기본으로 선택되고, 저장된 뉴스 카드가 보이면 정상입니다.
   - DB 에 데이터가 없으면 "해당 날짜에 저장된 뉴스가 없습니다." 메시지가 나타납니다.

---
## 8) 에러/로그 확인 포인트
- HTTP 호출 실패: `[구글 RSS 수집기 오류] HTTP 호출 실패` 로그를 확인합니다.
- pubDate 파싱 실패: `[구글 RSS 파서] pubDate 파싱 실패` 로그를 확인하고 원본 문자열을 확인하세요.
- DB 중복: `[중복 확인] 이미 저장된 기사` 로그는 UNIQUE KEY 정책이 잘 동작한다는 의미입니다.
- API 호출 오류: 화면에 "데이터를 불러오지 못했습니다. 서버 로그를 확인하세요." 메시지가 나오면 서버 로그에서 스택트레이스를 확인하세요.

---
## 코드 조각 위치 힌트
- 스케줄러 & 파서: `src/main/java/com/the198thstreet/news/google/service/GoogleHeadlineNewsService.java`
- JDBC 저장소: `src/main/java/com/the198thstreet/news/google/repository/HeadlineNewsRepository.java`
- REST API: `src/main/java/com/the198thstreet/news/google/controller/ArchiveHeadlineApiController.java`
- 화면 템플릿: `src/main/resources/templates/archive/headlines.html`
- 스키마/설정: `src/main/resources/schema.sql`, `src/main/resources/application.properties`

각 파일에는 풍부한 주석을 남겨두었으니, 해당 파일을 열어보며 본 문서를 병행해 읽으면 전체 흐름을 쉽게 파악할 수 있습니다.
