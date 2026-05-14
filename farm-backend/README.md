# farm-backend

관련 글:
- [트랜잭션 구조 개선](https://ats3059.tistory.com/44)
- [외부 API 호출 최적화](https://ats3059.tistory.com/45)

---

## 다루는 문제

| 문제 | 매핑 |
| --- | --- |
| 동기화 완료 알림이 먼저 가고 데이터는 나중에 보임 | `[문제 3]` |
| 1000마리 동기화에 15분 이상 | `[문제 4]` `[문제 5]` |
| 동기화 진행 중 다른 작업이 Lock Timeout | `[문제 1]` `[문제 2]` `[문제 8]` |
| 외부 API 장애와 서버 장애를 즉시 구분하지 못함 | `[문제 7]` |

각 문제는 [`ServiceFarmCowSyncLegacy`](src/main/java/com/farm/application/legacy/ServiceFarmCowSyncLegacy.java) 안에 `[문제 N]` 주석으로 박아두었습니다. 의도적으로 안티패턴을 남긴 코드라, `POST /farms/{id}/sync/legacy` 로 돌려서 개선판과 동일한 데이터셋에서 비교할 수 있습니다.

---

## 빠른 실행

MySQL 8 컨테이너:

```bash
docker run -d --name farm-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=farm_backend \
  -e MYSQL_USER=farm \
  -e MYSQL_PASSWORD=farm \
  -p 3306:3306 \
  mysql:8
```

부팅하면 `Farm("FARM-001", ...)` 한 건이 들어갑니다 (이미 있으면 그대로 둡니다). 멀티모듈 루트에서 실행합니다:

```bash
./gradlew :farm-backend:bootRun
```

비교 실행:

```bash
# 개선판 — 비동기 (202 Accepted, 백그라운드에서 BREED + BUTCHERY 진행)
curl -i -X POST http://localhost:8080/farms/1/sync

# 진행 현황 (목장당 단수 Job + chunk 들 상태)
curl http://localhost:8080/farms/1/sync/job | jq

# 레거시 재현
curl -i -X POST http://localhost:8080/farms/1/sync/legacy
```

---

## 1. 트랜잭션 경계 재정의

### 증상

긴 트랜잭션 안에서 외부 API 응답을 기다리면서 DB Lock 을 잡고 있다 보니, 인접 작업(목장 수정 같은 것)이 Lock Timeout 으로 줄줄이 실패했습니다. Push 는 트랜잭션 커밋 전에 발송돼서 "알림은 왔는데 목록은 비어있어요" 문의가 자주 올라왔습니다.

### Before — 단일 트랜잭션, 외부 API + DB + push 가 한 통에

```java
// ServiceFarmCowSyncLegacy
@Async
@Transactional   // [문제 1][문제 2] 외부 API 응답 대기 동안 DB Lock 점유
public void syncFarmCattle(Long farmId) {
  Farm farm = repositoryFarm.findById(farmId).orElseThrow();
  syncByState(farm, CattleState.BREED);     // 외부 API + DB SAVE 모두 이 안
  sendPushNaively(farm);                    // [문제 3] 커밋 전 push 발송
  syncByState(farm, CattleState.BUTCHERY);  // 외부 API + DB SAVE 모두 이 안
}
```

전부 하나의 `@Transactional` 안에 들어가 있습니다. 1000마리 외부 API 응답 대기 동안 그 트랜잭션이 살아 있고, 그 시간만큼 같은 row 를 건드리는 다른 작업이 Lock 대기로 쌓입니다.

### After — chunk 단위로 트랜잭션을 자르기

```java
// ServiceFarmAsync
@Async("farmSyncExecutor")   // @Transactional 없음 — 진입점은 트랜잭션 밖
public void syncFarmCattle(Long farmId) {
  Farm farm = repositoryFarm.findById(farmId).orElseThrow(...);
  Long jobId = serviceSyncJob.createJob(farm.getId());
  try {
    int linkedAfterBreed = runStateSync(jobId, farm, CattleState.BREED);
    pushNotifier.notifyBreedSyncDone(farm, linkedAfterBreed);    // chunk 들이 다 commit 된 뒤에만
    int linkedAfterButchery = runStateSync(jobId, farm, CattleState.BUTCHERY);
    pushNotifier.notifyButcherySyncDone(farm, linkedAfterButchery);
  } catch (RuntimeException ex) {
    log.error(...);
  } finally {
    serviceSyncJob.finishJob(jobId);   // 어디서 깨지든 Job 마감
  }
}
```

- 진입점에서 `@Transactional` 을 떼고, [`ServiceSyncJob`](src/main/java/com/farm/application/service/ServiceSyncJob.java) / [`ServiceCattlePersister`](src/main/java/com/farm/application/service/ServiceCattlePersister.java) 메서드들을 `Propagation.REQUIRES_NEW` 로 분리했습니다. 트랜잭션은 chunk 안쪽에서만 짧게 열고 닫힙니다.
- [`ComponentChunkPlanner`](src/main/java/com/farm/application/component/ComponentChunkPlanner.java) 가 입력을 100건짜리 chunk 로 자릅니다. chunk 1개 = 트랜잭션 1개. 한 chunk 가 깨져도 옆 chunk 의 정상 commit 은 살아 있습니다.
- Push 는 *상태별 chunk 들이 다 commit 된 뒤* 에만 호출됩니다. "알림 먼저, 데이터 나중" 사고를 차단합니다.
- finally 가드 덕에 어디서 RuntimeException 이 터져도 `finishJob` 은 무조건 호출됩니다. Job 이 RUNNING 인 채로 영구화되는 사고를 막습니다.

### 도메인 — 감사 가능한 Job/Chunk

목장 인증은 1회성 이벤트라 [`SyncJob`](src/main/java/com/farm/application/domain/SyncJob.java) 은 farmId 에 unique 합니다. 같은 Job 아래로 [`SyncChunk`](src/main/java/com/farm/application/domain/SyncChunk.java) 들이 BREED / BUTCHERY 라벨로 적층되고, 각 chunk 는 `cattleNos` 본문을 들고 있어 단위로 감사할 수 있습니다.
부분 실패 시 실패한 chunk 만 `FAILED` / `PARTIAL`, 나머지는 `DONE`, Job 은 `PARTIALLY_FAILED` 로 마감됩니다.

---

## 2. 외부 호출 — 줄이고, 병렬화하고, 안정시키기

### 증상

1000마리 목장 동기화에 15분이 걸렸습니다. 까보니 DB 에 이미 800마리가 캐시돼 있는데도 모든 개체에 대해 외부 API 가 호출되고 있었습니다. 호출은 직렬, 단일 실패도 즉시 전체 동기화를 깼습니다.

### 줄이기 — DB 선조회로 N+1 제거

레거시는 개체 한 마리당 `findByCattleNo` 한 번을 직렬로 돌렸습니다. 1000마리면 SELECT 1000번이 트랜잭션 안에서 발생합니다.

```java
// Before
for (String cattleNo : cattleNoList) {
  Cattle cattle = repositoryCattle.findByCattleNo(cattleNo).orElse(null);  // [문제 4] N+1
  if (cattle == null) {
    Optional<CattleTraceData> detail = apiCattleTrace.findCattleDetail(cattleNo);  // [문제 5]
    ...
  }
}
```

[`ServiceCattleFilter.split()`](src/main/java/com/farm/application/service/ServiceCattleFilter.java) 가 `findByCattleNoIn(...)` 한 번으로 입력을 두 통으로 가릅니다.

```java
// After
public CattleSplit split(List<String> cattleNoList) {
  Map<String, Cattle> existing = repositoryCattle.findByCattleNoIn(cattleNoList).stream()
    .collect(Collectors.toMap(Cattle::getCattleNo, Function.identity()));
  List<String> missing = cattleNoList.stream()
    .filter(cattleNo -> !existing.containsKey(cattleNo))
    .toList();
  return new CattleSplit(existing, missing);
}
```

- `existing` — 이미 우리 DB 에 있는 개체. 외부 호출이 필요 없습니다.
- `missing` — 외부 호출이 필요한 개체.

같은 N+1 패턴이 사실 `FarmCattle` 중복 체크 쪽에도 또 있었습니다. `countByFarmAndCattle` 을 마리별로 돌리던 걸 IN 쿼리 한 방으로 끊어서, **chunk 당 조회 수가 200여 번에서 2번으로 줄었습니다**.

### 병렬화 — CompletableFuture + RateLimiter + 단건 timeout

[`ComponentParallelCattleFetch.fetchAll()`](src/main/java/com/farm/application/component/ComponentParallelCattleFetch.java) 이 `missing` 만 병렬로 던집니다.

```java
List<CompletableFuture<Void>> futures = cattleNoList.stream()
  .map(cattleNo -> CompletableFuture
    .supplyAsync(() -> client.findCattleDetail(cattleNo), executor)
    .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)   // 한 건이 매달려도 batch 가 풀림
    .thenAccept(detail -> {
      if (detail.isEmpty()) notFound.add(cattleNo);
      else fetched.put(cattleNo, detail.get());
    })
    .exceptionally(ex -> { failedAfterRetry.add(cattleNo); return null; }))
  .toList();
CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
```

단건 외부 호출은 [`ComponentRateLimitedTraceClient`](src/main/java/com/farm/application/component/ComponentRateLimitedTraceClient.java) 를 통과합니다 — `@RateLimiter` + `@CircuitBreaker` 로 외부 시스템을 보호합니다. 결과는 세 통으로 분류됩니다.

- `fetched` — 응답 정상
- `notFound` — 외부에 정보가 없는 정상 케이스
- `failedAfterRetry` — 응답 자체를 못 받은 케이스 (Slack 통보 후 chunk 가 PARTIAL/FAILED 로 마감)

### 안정시키기 — 재시도 / CB / Slack 한 곳에서

외부 호출은 [`ComponentExternalApiTemplate`](src/main/java/com/farm/application/common/ComponentExternalApiTemplate.java) 한 곳을 지나갑니다.

```java
public <T> T execute(String operationName, Supplier<T> call) {
  try {
    return Retry.decorateSupplier(retry, call).get();
  } catch (RuntimeException ex) {
    notifySlackQuietly(operationName, ex);   // Slack 호출은 별도 try/catch 격리
    throw ex;
  }
}
```

- Resilience4j Retry 모듈에 위임해서 재시도합니다. 시도마다 로그가 남아서 "1번 만에 깨진 건지 3번까지 갔는지" 운영 단에서 바로 구분할 수 있습니다.
- 재시도를 다 써도 안 되면 Slack 으로 통보합니다. Slack 호출은 따로 try/catch 로 격리해서, Slack 자체가 죽어도 원본 외부 API 예외를 가리지 않게 했습니다.
- CircuitBreaker OPEN 또는 retry 소진 시 fallback 이 [`ExternalCattleFetchFailedException`](src/main/java/com/farm/application/external/ExternalCattleFetchFailedException.java) sentinel 을 throw 합니다 → ParallelFetch 의 `.exceptionally()` 가 잡아서 `failedAfterRetry` 통으로 분류합니다. "외부에 정보 없음(notFound)" 과 "응답을 못 받음(failedAfterRetry)" 이 명확히 갈립니다.

### 스레드 풀 분리

[`ConfigThread`](src/main/java/com/farm/infrastructure/config/ConfigThread.java) 에서 두 풀을 분리했습니다.

- `farmSyncExecutor` — 목장 단위 비동기 진입점 (core 2 / max 4)
- `cattleFetchExecutor` — chunk 안의 외부 API 병렬 호출 (core 8 / max 16)

외부 호출 적체가 목장 단위 진입점이나 톰캣 워커 풀로 전이되지 않게 격벽을 둡니다.

---

## 데모 환경 메모

- 외부 축산 이력 시스템은 [`ApiCattleTrace`](src/main/java/com/farm/application/external/ApiCattleTrace.java) 인터페이스로만 노출합니다. [`ApiCattleTraceDemoImpl`](src/main/java/com/farm/infrastructure/external/ApiCattleTraceDemoImpl.java) 가 고정된 가짜 응답(목장 1개당 BREED 200 + BUTCHERY 50)을 돌려줍니다.
- Slack Webhook 발송은 [`ComponentSlackMessenger`](src/main/java/com/farm/application/common/ComponentSlackMessenger.java) 안에서 로그 출력으로 대체했습니다.
- 운영 DB 는 MySQL 8.
- 인증/권한은 핵심 동기화 흐름 시연에 필요 없어서 생략했습니다.

---

## 패키지 구조

```
src/main/java/com/farm
├── presentation/                        REST 진입점
│   └── ControllerFarmSync
│
├── application/
│   ├── service/                         비즈니스 흐름
│   │   ├── ServiceFarmAsync             진입점 (no @Transactional, finally 가드)
│   │   ├── ServiceSyncJob               REQUIRES_NEW Job/Chunk 라이프사이클
│   │   ├── ServiceCattlePersister       REQUIRES_NEW chunk 저장
│   │   ├── ServiceCattleFilter          DB 선조회 (split: existing / missing)
│   │   └── ServiceFarmSyncQuery         조회 전용 (목장당 단수 Job)
│   ├── component/                       단위 책임
│   │   ├── ComponentChunkPlanner        100단위 chunk 분할
│   │   ├── ComponentRateLimitedTraceClient   @RateLimiter + @CircuitBreaker
│   │   ├── ComponentParallelCattleFetch      CompletableFuture + 단건 timeout
│   │   └── ComponentFarmPushNotifier    chunk commit 후 발송
│   ├── common/                          공통 어댑터
│   │   ├── ComponentExternalApiTemplate Retry + Slack
│   │   └── ComponentSlackMessenger
│   ├── domain/                          엔티티 + enum + value
│   ├── dto/                             API 응답 record
│   ├── external/                        외부 인터페이스 + 도메인 예외
│   └── legacy/                          ServiceFarmCowSyncLegacy (안티패턴 보존)
│
└── infrastructure/
    ├── persistence/                     Spring Data JPA 어댑터
    ├── external/                        외부 API 실제 구현 (Demo)
    └── config/                          @Configuration
        ├── ConfigThread                 두 풀 분리
        └── InitialDataInjector          부팅 시 데모용 Farm 한 건
```

---