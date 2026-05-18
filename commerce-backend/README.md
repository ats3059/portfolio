# Commerce Backend

주문 자체보다 **결제 승인 시점에 재고가 꼬이지 않게 처리하는 일** 과 **결제 상태를 토스 웹훅까지 안정적으로 따라가게 만드는 일** 에 더 무게를 둔 백엔드 예제입니다. **재고 동시성 / 결제 전략 / 토스 웹훅 전략** 세 축이 중심입니다.

한 주문 안에 여러 판매자의 상품이 같이 담기는 5계층 구조(`Payment → Order → OrderVendor → OrderProduct → OrderItem`) 와, 결제 수단·웹훅 상태가 늘어나도 기존 코드를 많이 건드리지 않게 분리한 전략 패턴이 같이 들어가 있습니다.

## 빠른 실행

멀티모듈 루트에서 실행합니다. H2 인메모리 DB 를 쓰니까 별도 인프라 없이 바로 실행됩니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :commerce-backend:bootRun
```

## 1. 재고 동시성 처리

재고가 음수로 떨어지거나 실제보다 많이 팔리는 상황을 막아야 하는데, 주문 생성 시점부터 재고를 건드리면 결제 실패 뒤 정리할 게 많아집니다. 그래서 주문을 만들 때는 재고를 그대로 두고, 결제 승인 트랜잭션이 열릴 때만 `StockService` 가 차감을 맡도록 정리했습니다.

`PaymentService.startPayment(...)` 안에서 `OrderItemFinder` 가 해당 주문의 모든 OrderItem 을 모으고, `StockService` 가 `ProductVariantRepository#findAllByIdInForUpdate(...)` 한 번에 `PESSIMISTIC_WRITE` 락을 걸고 차감합니다. 한 item 이라도 재고가 부족하면 결제 전체를 `ApiConflictException` 으로 실패 처리하고, 같은 트랜잭션이라 차감이 같이 롤백됩니다.

자세한 내용: [docs/case-studies/재고-동시성.md](docs/case-studies/재고-동시성.md)

## 2. 결제 모듈 구조

한 주문 안에 여러 판매자 상품이 같이 담기고 결제 수단도 계속 늘어날 수 있다는 전제로 구조를 잡았습니다. 결제·주문은 `Payment → Order → OrderVendor → OrderProduct → OrderItem` 5계층으로 나눠서 결제 / 판매자 / 상품 / 옵션 경계가 코드에서 바로 읽히게 했고, 결제는 `PaymentContext` 가 `CARD`, `VIRTUAL_ACCOUNT` 전략을 골라 승인하도록 분리했습니다. 새 결제 수단이 추가돼도 컨텍스트는 그대로 두고 `PaymentStrategy` 구현체만 하나 더 붙이면 됩니다.

엔티티 간 참조는 ID 컬럼 + 인덱스로만 잡고, JPA `@OneToMany` / `@ManyToOne` 객체 그래프 매핑이나 DB FK 제약은 사용하지 않습니다. 마이그레이션·서비스 분리 유연성을 살리기 위한 결정입니다.

자세한 내용: [docs/case-studies/결제-모듈-구조.md](docs/case-studies/결제-모듈-구조.md)

## 3. 토스 웹훅 처리

가상계좌 결제는 PG 가 동기 응답으로 끝낼 수 없습니다. 사용자가 입금하면 토스가 웹훅으로 통보해 주는데, 같은 엔드포인트로 입금 대기(`WAITING_FOR_DEPOSIT`) / 입금 완료(`DONE`) / 취소(`CANCELED`) / 중단(`ABORTED`) / 만료(`EXPIRED`) 등 다양한 상태가 떨어집니다. 결제 전략과 같은 결로, 웹훅도 `TossWebHookStatusStrategy` 인터페이스 + 상태별 구현체(`DoneStrategy`, `WaitingForDepositStrategy`, `CanceledStrategy`, `AbortedStrategy`, `ExpiredStrategy`) 로 분리했고, `TossWebHookContext` 가 들어온 상태에 맞는 전략을 골라 디스패치합니다.

웹훅은 실패 시 다시 들어올 수 있으므로, **도메인 메서드(`Payment#confirm()` / `cancel()` / `waitForDeposit()` / `abort()` / `expire()`) 를 자체적으로 멱등하게** 만들어 같은 상태 전이가 반복돼도 결과가 바뀌지 않게 했습니다. 별도 멱등성 가드 테이블·캐시 없이도 같은 웹훅을 안전하게 다시 처리할 수 있습니다.

엔드포인트는 `POST /v1/payment/toss/webhook/payment-status-changed` 입니다. `TossWebHookFilter`(`OncePerRequestFilter`) 가 Content-Type 을 검증하고 수신 로그를 남깁니다.

자세한 내용: [docs/case-studies/토스-웹훅-처리.md](docs/case-studies/토스-웹훅-처리.md)

## 공개 API

- `POST /api/orders` — 주문 생성 (5계층 한 트랜잭션)
- `POST /api/orders/payments` — 결제 시작 (재고 차감 + 전략 디스패치 + 주문 상태 전이)
- `POST /v1/payment/toss/webhook/payment-status-changed` — 토스 웹훅 수신
