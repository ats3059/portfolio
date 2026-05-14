package com.farm.application.component;

import com.farm.application.domain.Farm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 동기화 완료 push 발송 추상화.
 * 레거시는 트랜잭션이 커밋되기도 전에 push 를 보냈고
 * "알림은 왔는데 목록이 비어있어요" 문의가 잦았다.
 * 이 클래스는 chunk 들이 모두 commit 된 뒤에만 호출되도록 호출 시점을 강제한다.
 */
@Component
@Slf4j
public class ComponentFarmPushNotifier {

  public void notifyBreedSyncDone(Farm farm, int totalLinked) {
    log.info("[push] 사육 동기화 완료 farmer={}, linked={}", farm.getFarmerName(), totalLinked);
  }

  public void notifyButcherySyncDone(Farm farm, int totalLinked) {
    log.info("[push] 도축 동기화 완료 farmer={}, linked={}", farm.getFarmerName(), totalLinked);
  }
}
