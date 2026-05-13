package com.farm.application.domain;

import com.farm.application.domain.Cattle;
import java.util.List;
import java.util.Map;

/**
 * DB 선조회로 갈라낸 결과.
 *
 *  - existing  : 이미 우리 쪽에 캐시된 개체 (외부 API 호출 불필요)
 *  - missing   : 외부 이력 시스템 호출이 필요한 개체번호
 *
 * 레거시 구현은 매 개체마다 외부 API 를 직렬로 호출했다.
 * 이 split 한 번으로 1000마리 중 800마리가 existing 쪽에 떨어지면
 * 외부 호출이 그만큼 증발한다.
 */
public record CattleSplit(
  Map<String, Cattle> existing,
  List<String> missing
) {
}
