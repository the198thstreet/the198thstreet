# 코드 흐름 총정리 (완전 초보용)

> **목표**: 네이버 뉴스나 DB 없이, 메모리 `Map` 만으로 동작하는 초간단 스케줄러가 어떻게 돌아가는지 눈으로 따라가기.

---

## 1. 실행 준비
- **필수 조건 없음**: 외부 API 키, 데이터베이스 서버 모두 필요 없습니다.
- **실행 명령**: 터미널에서 아래 한 줄이면 끝입니다.
  ```bash
  ./mvnw spring-boot:run
  ```
- **왜 이렇게 간단한가?**
  - 애플리케이션이 내부에서 **샘플 기사 데이터**를 만들고 메모리에 넣기 때문입니다.
  - 실패 지점이 없어 "실행 → 로그 확인"만으로 전체 흐름을 익힐 수 있습니다.

## 2. 주요 파일과 하는 일
- `src/main/java/com/the198thstreet/SimpleNewsScheduler.java`
  - 스케줄러가 1분마다 실행되며, 샘플 기사를 `Map<Integer, Map<String, String>>` 형태로 메모리에 채웁니다.
  - 네이버/구글 API, 엔티티, 리포지토리, DB 연결 **모두 제거**.
- `src/main/resources/application.properties`
  - `news.collector.memory.enabled` : 스케줄러 켤지/끌지 선택.
  - `news.collector.memory.fixed-rate-ms` : 몇 ms마다 실행할지 설정.
- `src/main/resources/schema.sql`
  - 현재 버전에서는 DB가 필요 없음을 설명하는 주석만 남겨둔 상태입니다.

## 3. 스케줄러가 자동으로 돈다고?
- `The198thstreetApplication` 클래스에 `@EnableScheduling`이 달려 있어서, 스프링이 부팅될 때 스케줄러를 자동 등록합니다.
- 등록된 메서드: `SimpleNewsScheduler#refreshNewsCache`
  - `@Scheduled(fixedRateString = "${news.collector.memory.fixed-rate-ms:60000}")`
  - `application.properties` 값을 읽어서 주기를 결정합니다. 값을 바꾸면 **다음 실행부터 바로 반영**됩니다.

## 4. 한 사이클의 내부 흐름 (로그를 따라가기)
1. **시작 로그** : `[메모리 뉴스 스케줄러 시작] 실행번호=… 주기=…ms`
2. `buildSampleHeadlines`가 호출되어 기사 3개를 만듭니다.
   - 각 기사는 `title`, `summary`, `source`, `collectedAt`, `executionNo` 키를 가진 `Map<String, String>` 입니다.
3. `inMemoryNews`를 `clear()` 해서 이전 실행 데이터를 싹 비웁니다.
4. 새 기사 Map 을 순번(1,2,3)에 맞춰 `ConcurrentHashMap`에 넣습니다.
   - DEBUG 로그로 `[기사 저장] 순번=… 내용=…` 이 찍힙니다.
5. **완료 로그** : `[메모리 뉴스 스케줄러 완료] 실행번호=… 저장된 건수=3 소요시간=…ms`

## 5. 데이터를 어떻게 확인하나요?
- 현재는 콘솔 로그로만 흐름을 확인하도록 구성했습니다.
- `SimpleNewsScheduler#getCurrentNewsSnapshot()`을 호출하면, 스케줄러가 메모리에 들고 있는 Map을 **복사본**으로 돌려받을 수 있습니다.
  - 나중에 REST API나 테스트 코드에서 쉽게 재사용할 수 있도록 분리했습니다.

## 6. 자주 묻는 질문 (FAQ)
- **Q. 왜 Map을 썼나요?**
  - 목적은 "가장 단순한 데이터 흐름"을 보여주는 것입니다. 엔티티/DTO를 모르더라도 key-value 구조만 알면 전부 이해할 수 있습니다.
- **Q. DB나 외부 API를 붙이고 싶다면?**
  - 스케줄러 안의 `buildSampleHeadlines` 부분을 원하는 데이터로 교체하고, `inMemoryNews` 대신 JPA 리포지토리 등을 주입하면 됩니다. 지금 구조는 의존성이 적어 쉽게 확장할 수 있습니다.
- **Q. 실행 번호(executionNo)는 왜 넣었나요?**
  - 스케줄러가 몇 번째로 실행된 결과인지 로그와 데이터에서 한눈에 볼 수 있게 하기 위해서입니다. 문제 상황을 재현하거나 설명할 때 유용합니다.

---
이 문서 하나만 읽고도 "애플리케이션이 켜지면 스케줄러가 1분마다 메모리 Map을 갱신한다"는 흐름을 완전히 이해할 수 있습니다.
