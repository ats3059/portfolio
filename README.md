# backend portfolio

백엔드 포트폴리오 두 프로젝트를 Gradle 멀티모듈로 묶은 레포입니다. 두 모듈은 코드/설정을 공유하지 않고 각자 독립 빌드됩니다.

| 모듈 | 다루는 영역 | 위치 |
| --- | --- | --- |
| **farm-backend** | 트랜잭션 경계 / 외부 API 호출 / chunk 단위 비동기 동기화 | [`farm-backend/`](farm-backend/) |
| **commerce-backend** | 재고 동시성 / 결제 전략 / 토스 웹훅 멱등 처리 | [`commerce-backend/`](commerce-backend/) |

---

## farm-backend

목장 동기화 시스템에서 단일 트랜잭션 + 외부 API 직렬 호출이 만들던 Lock Timeout / 1000마리 15분 / "알림 먼저, 데이터 나중" 같은 문제를 chunk 단위 트랜잭션 + DB 선조회 + 병렬 호출로 풀어낸 흐름입니다.

- `@Transactional` 을 진입점에서 떼고 `REQUIRES_NEW` 로 chunk 안쪽에만 트랜잭션을 둡니다
- `findByCattleNoIn` 으로 N+1 SELECT 를 끊었습니다 (chunk 당 200여 → 2)
- `CompletableFuture` + `@RateLimiter` + `@CircuitBreaker` 로 외부 호출을 병렬화하고 보호합니다
- Resilience4j Retry 모듈로 재시도하며, 실패 시 알림 채널로 통보합니다

→ [`farm-backend/README.md`](farm-backend/README.md)

## commerce-backend

한 주문 안에 여러 판매자 상품이 같이 담기는 5계층 구조와, 결제 / 토스 웹훅 흐름을 전략 패턴으로 정리한 흐름입니다.

- `Payment → Order → OrderVendor → OrderProduct → OrderItem` 5계층 + ID 참조
- 결제 승인 시점에만 `PESSIMISTIC_WRITE` 락으로 재고를 차감합니다 → 동시성 안전
- `PaymentContext` + `PaymentStrategy` 로 카드 / 가상계좌를 분리했고, 새 결제 수단이 추가돼도 컨텍스트는 무수정입니다
- 토스 웹훅을 상태별 전략으로 디스패치하며, 도메인 메서드 자체를 멱등하게 만들어 별도 가드 없이 재시도가 안전합니다

→ [`commerce-backend/README.md`](commerce-backend/README.md)

---

## 빠른 실행

루트에서 한 번에 빌드하거나, 모듈 단위로 실행할 수 있습니다.

```bash
# 전체 빌드
./gradlew build

# farm 만 — MySQL 8 컨테이너 필요 (farm-backend/README.md 참고)
./gradlew :farm-backend:bootRun

# commerce 만 — H2 인메모리, 추가 인프라 불필요
SPRING_PROFILES_ACTIVE=local ./gradlew :commerce-backend:bootRun
```

IntelliJ 에서는 루트 폴더를 열면 두 모듈을 자동 인식합니다.
