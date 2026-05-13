package com.farm.application.component;

import com.farm.application.common.ComponentExternalApiTemplate;
import com.farm.application.external.ApiCattleTrace;
import com.farm.application.external.CattleTraceData;
import com.farm.application.external.ExternalCattleFetchFailedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 이력 시스템 단건 호출을 RateLimiter + CircuitBreaker 로 보호하고
 * ComponentExternalApiTemplate 을 통해 재시도/알림까지 일관되게 묶는다.
 *
 * <p>RateLimiter: 단위 시간 호출 수 상한 (호출 폭주 방지)
 * <br>CircuitBreaker: 실패율/슬로우 호출 비율로 서킷 OPEN (외부 시스템 장애 시 차단)
 *
 * <p>AOP 프록시가 적용돼야 하므로 호출자는 반드시 이 컴포넌트를 주입받아 호출해야 한다.
 * (같은 클래스 내부 호출은 프록시를 우회해 보호 정책이 동작하지 않는다.)
 *
 * <p>fallback 은 "외부 시스템 응답을 끝내 못 받음" 을 의미하는
 * {@link ExternalCattleFetchFailedException} 을 throw 한다. 이 예외는
 * ComponentParallelCattleFetch 의 .exceptionally() 가 잡아 failedAfterRetry 로 분류한다.
 * "외부에 정보 없음(Optional.empty())" 이라는 정상 케이스와 명확히 구분된다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentRateLimitedTraceClient {

  static final String RESILIENCE_NAME = "farmExternalApi";

  private final ApiCattleTrace apiCattleTrace;
  private final ComponentExternalApiTemplate apiTemplate;

  @RateLimiter(name = RESILIENCE_NAME)
  @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "fallbackFindCattleDetail")
  public Optional<CattleTraceData> findCattleDetail(String cattleNo) {
    return apiTemplate.execute(
      "ApiCattleTrace.findCattleDetail",
      () -> apiCattleTrace.findCattleDetail(cattleNo)
    );
  }


  /**
   * CircuitBreaker OPEN 또는 재시도 후 실패 시 fallback.
   * sentinel 예외를 throw 해 호출자(ComponentParallelCattleFetch)의 .exceptionally() 분기로 흘려보낸다.
   */
  @SuppressWarnings("unused")
  private Optional<CattleTraceData> fallbackFindCattleDetail(String cattleNo, Throwable e) {
    log.warn("ApiCattleTrace.findCattleDetail fallback cattleNo={}, reason={}", cattleNo, e.getMessage());
    throw new ExternalCattleFetchFailedException(cattleNo, e);
  }
}
