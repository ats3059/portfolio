package com.farm.application.external;

import com.farm.application.domain.Cattle;
import java.time.LocalDate;

/**
 * 외부 이력 시스템에서 내려오는 개체 정보.
 */
public record CattleTraceData(
  String cattleNo,
  Cattle.Gender gender,
  LocalDate birthDate
) {
}
