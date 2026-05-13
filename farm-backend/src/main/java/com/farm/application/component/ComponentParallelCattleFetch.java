package com.farm.application.component;

import com.farm.application.domain.CattleFetchOutcome;
import com.farm.application.external.CattleTraceData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 이력 시스템 호출을 CompletableFuture 로 병렬 처리한다.
 *
 *  - 단건 호출 자체는 ComponentRateLimitedTraceClient 가 RateLimiter + 재시도 + Slack 통보까지 책임짐
 *  - 이 클래스는 "여러 건을 어떻게 동시에 던질지" 와 "결과를 어떻게 모을지" 만 담당
 *  - 필드명 cattleFetchExecutor 는 ConfigThread 의 @Bean 이름과 일치 → Spring 이 자동 매칭 (Qualifier 불필요)
 *
 *  결과 분류:
 *   - fetched          : 외부 시스템이 정상 응답한 cattle
 *   - notFound         : 외부 시스템이 "정보 없음" 을 명시적으로 응답한 cattle (정상 흐름)
 *   - failedAfterRetry : 재시도 소진 / CircuitBreaker OPEN 등으로 응답 자체를 못 받은 cattle
 *                        (ComponentRateLimitedTraceClient 가 던지는 ExternalCattleFetchFailedException
 *                         또는 timeout 예외가 .exceptionally() 분기로 흘러옴)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ComponentParallelCattleFetch {

  static final int TIMEOUT_SECONDS = 60;

  private final ComponentRateLimitedTraceClient client;
  private final Executor cattleFetchExecutor;

  public CattleFetchOutcome fetchAll(List<String> cattleNoList) {
    if (cattleNoList.isEmpty()) {
      return new CattleFetchOutcome(Map.of(), List.of(), List.of());
    }

    Map<String, CattleTraceData> fetched = new ConcurrentHashMap<>();
    List<String> notFound = Collections.synchronizedList(new ArrayList<>());
    List<String> failedAfterRetry = Collections.synchronizedList(new ArrayList<>());

    List<CompletableFuture<Void>> futures = cattleNoList.stream()
      .map(cattleNo -> CompletableFuture
        .supplyAsync(() -> client.findCattleDetail(cattleNo), cattleFetchExecutor)
        .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .thenAccept(detail -> {
          if (detail.isEmpty()) {
            notFound.add(cattleNo);
          } else {
            fetched.put(cattleNo, detail.get());
          }
        })
        .exceptionally(ex -> {
          log.warn("외부 API 호출 종료 실패 cattleNo={}, type={}, reason={}",
            cattleNo, ex.getClass().getSimpleName(), ex.getMessage());
          failedAfterRetry.add(cattleNo);
          return null;
        }))
      .toList();

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    return new CattleFetchOutcome(
      Map.copyOf(fetched),
      List.copyOf(notFound),
      List.copyOf(failedAfterRetry)
    );
  }
}
