package com.farm.application.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 연동 실패 알림 채널 추상화.
 *
 * 실제 환경에서는 Slack Webhook 으로 발송하지만, 이 레포에서는
 * 외부 의존 없이 돌아가도록 로그 출력으로 대체한다.
 */
@Component
@Slf4j
public class ComponentSlackMessenger {

  public void sendToSystemChannel(String title, String message) {
    log.warn("[slack:system] {} | {}", title, message);
  }
}
