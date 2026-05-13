package com.farm.application.service;

import com.farm.application.domain.Cattle;
import com.farm.application.domain.CattleSplit;
import com.farm.infrastructure.persistence.JpaRepositoryCattle;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 API 호출 전에 DB 를 한 번 훑어서 이미 동기화된 개체를 골라낸다.
 *
 * 레거시는 개체 하나당 findByCattleNo 를 호출해 N+1 SELECT 가 났고,
 * 그 결과 "어차피 DB 에 있을 개체"에 대해서도 외부 API 가 호출됐다.
 * 여기서는 findByCattleNoIn 한 번으로 끊어낸다.
 */
@Service
@RequiredArgsConstructor
public class ServiceCattleFilter {

  private final JpaRepositoryCattle repositoryCattle;

  @Transactional(readOnly = true)
  public CattleSplit split(List<String> cattleNoList) {
    if (cattleNoList.isEmpty()) {
      return new CattleSplit(Map.of(), List.of());
    }

    Map<String, Cattle> existing = repositoryCattle.findByCattleNoIn(cattleNoList).stream()
      .collect(Collectors.toMap(Cattle::getCattleNo, Function.identity()));

    List<String> missing = cattleNoList.stream()
      .filter(cattleNo -> !existing.containsKey(cattleNo))
      .toList();

    return new CattleSplit(existing, missing);
  }
}
