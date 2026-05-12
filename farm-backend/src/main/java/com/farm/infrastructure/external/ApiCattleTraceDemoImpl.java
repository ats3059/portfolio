package com.farm.infrastructure.external;

import com.farm.application.domain.Cattle;
import com.farm.application.domain.CattleState;
import com.farm.application.external.ApiCattleTrace;
import com.farm.application.external.CattleTraceData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출을 대체하는 데모 구현.
 * 포트폴리오 실행용 가짜 응답을 내려준다.
 *
 *  - 목장 1개당 BREED 200 + BUTCHERY 50 마리의 결정적인 cattleNo 를 반환
 *  - 단건 호출은 외부 API 응답 지연을 모사하기 위해 짧게 sleep
 */
@Component
public class ApiCattleTraceDemoImpl implements ApiCattleTrace {

  private static final long LATENCY_MILLIS = 5L;

  @Override
  public List<String> listFarmCattleNo(String farmNo, CattleState state) {
    int size = state == CattleState.BREED ? 200 : 50;
    List<String> cattleNoList = new ArrayList<>(size);
    for (int i = 1; i <= size; i++) {
      cattleNoList.add("%s-%s-%04d".formatted(farmNo, state.name(), i));
    }
    return cattleNoList;
  }

  @Override
  public Optional<CattleTraceData> findCattleDetail(String cattleNo) {
    sleepQuietly(LATENCY_MILLIS);

    Cattle.Gender gender = cattleNo.hashCode() % 2 == 0 ? Cattle.Gender.FEMALE : Cattle.Gender.MALE;
    LocalDate birthDate = LocalDate.now().minusMonths(Math.floorMod(cattleNo.hashCode(), 60) + 12L);
    return Optional.of(new CattleTraceData(cattleNo, gender, birthDate));
  }

  private void sleepQuietly(long millis) {
    if (millis <= 0) return;
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
