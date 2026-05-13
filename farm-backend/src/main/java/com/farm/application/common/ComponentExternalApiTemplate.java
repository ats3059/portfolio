package com.farm.application.common;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출 공통 모듈.
 *
 *  - Resilience4j Retry 모듈에 위임 → 백오프 + 지터 (resilience4j.retry.* 설정)
 *  - 재시도 시도마다 onRetry 이벤트로 attempt 번호 + 사유를 로그에 남긴다
 *  - 재시도 소진 시 마지막 예외를 잡아 Slack 시스템 채널로 통보 후 재전파
 *  - Slack 호출은 별도 try/catch 로 격리한다 — Slack 장애가 원본 외부 API 예외를 가리지 않게
 *  - 호출자는 외부 API 문제와 서버 문제를 알림 메시지로 구분할 수 있다
 */
@Component
@Slf4j
public class ComponentExternalApiTemplate {

  static final String RETRY_INSTANCE_NAME = "farmExternalApi";

  private final ComponentSlackMessenger slackMessenger;
  private final Retry retry;

  public ComponentExternalApiTemplate(ComponentSlackMessenger slackMessenger, RetryRegistry retryRegistry) {
    this.slackMessenger = slackMessenger;
    this.retry = retryRegistry.retry(RETRY_INSTANCE_NAME);
    this.retry.getEventPublisher()
      .onRetry(event -> log.warn(
        "외부 API 재시도 attempt={}/{}, waitDuration={}, reason={}",
        event.getNumberOfRetryAttempts(),
        retry.getRetryConfig().getMaxAttempts(),
        event.getWaitInterval(),
        event.getLastThrowable() == null ? "n/a" : event.getLastThrowable().getMessage()
      ));
  }

  public <T> T execute(String operationName, Supplier<T> call) {
    try {
      return Retry.decorateSupplier(retry, call).get();
    } catch (RuntimeException ex) {
      log.warn("외부 API 호출 최종 실패 operation={}, reason={}", operationName, ex.getMessage());
      notifySlackQuietly(operationName, ex);
      throw ex;
    }
  }

  private void notifySlackQuietly(String operationName, RuntimeException originalError) {
    try {
      slackMessenger.sendToSystemChannel(
        "외부 API 호출 최종 실패",
        "operation=%s, cause=%s".formatted(operationName, originalError.getMessage())
      );
    } catch (Exception slackError) {
      // Slack 장애가 원본 외부 API 예외를 가리지 않도록 여기서 삼킨다.
      log.error("Slack 통보 실패 operation={}, slackReason={}", operationName, slackError.getMessage(), slackError);
    }
  }
}
