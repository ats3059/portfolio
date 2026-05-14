package com.farm.application.service;

import com.farm.application.domain.Cattle;
import com.farm.application.domain.CattleFetchOutcome;
import com.farm.application.domain.CattleSplit;
import com.farm.application.domain.CattleState;
import com.farm.application.domain.Farm;
import com.farm.application.domain.FarmCattle;
import com.farm.application.external.CattleTraceData;
import com.farm.infrastructure.persistence.JpaRepositoryCattle;
import com.farm.infrastructure.persistence.JpaRepositoryFarm;
import com.farm.infrastructure.persistence.JpaRepositoryFarmCattle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chunk 한 개를 DB 에 반영하는 책임만 가진다.
 *
 *  - 외부 API 호출은 이미 끝난 상태여서 이 메서드 안에서 네트워크 호출이 일어나지 않는다
 *  - 트랜잭션 시간이 수백 ms 단위로 짧게 유지된다 → Lock 점유 시간 최소화
 *  - 동일 cattleNo 가 다른 chunk 와 경합해도 unique 제약으로 한쪽만 살리고 재조회한다
 *  - Farm 은 외래키만 필요해 getReferenceById 로 proxy 만 받는다 (chunk 마다 SELECT 회피)
 *  - FarmCattle 중복 체크는 단일 IN 쿼리로 묶어 chunk size 만큼의 N+1 SELECT 를 제거한다
 */
@Service
@RequiredArgsConstructor
public class ServiceCattlePersister {

  private final JpaRepositoryFarm repositoryFarm;
  private final JpaRepositoryCattle repositoryCattle;
  private final JpaRepositoryFarmCattle repositoryFarmCattle;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChunkPersistSummary persistChunk(
    Long farmId,
    CattleState state,
    CattleSplit split,
    CattleFetchOutcome outcome
  ) {
    Farm farm = repositoryFarm.getReferenceById(farmId);

    Map<String, Cattle> known = new HashMap<>(split.existing());
    int newlyInserted = 0;

    for (Map.Entry<String, CattleTraceData> entry : outcome.fetched().entrySet()) {
      Cattle saved = upsertCattle(entry.getValue());
      known.put(entry.getKey(), saved);
      newlyInserted += 1;
    }

    int newlyLinked = linkNewFarmCattle(farm, state, known.values());

    return new ChunkPersistSummary(newlyInserted, newlyLinked, outcome.notFound().size(), outcome.failedAfterRetry().size());
  }

  private int linkNewFarmCattle(Farm farm, CattleState state, Collection<Cattle> cattles) {
    if (cattles.isEmpty()) return 0;

    Set<Long> cattleIds = cattles.stream().map(Cattle::getId).collect(Collectors.toSet());
    // Hibernate proxy 의 id 접근은 초기화를 유발하지 않아 SELECT 가 발생하지 않는다.
    Set<Long> alreadyLinked = repositoryFarmCattle.findLinkedCattleIds(farm.getId(), cattleIds);

    List<FarmCattle> toSave = new ArrayList<>(cattleIds.size() - alreadyLinked.size());
    for (Cattle cattle : cattles) {
      if (alreadyLinked.contains(cattle.getId())) continue;
      toSave.add(new FarmCattle(farm, cattle, state));
    }
    repositoryFarmCattle.saveAll(toSave);
    return toSave.size();
  }

  private Cattle upsertCattle(CattleTraceData data) {
    try {
      return repositoryCattle.save(new Cattle(data.cattleNo(), data.gender(), data.birthDate()));
    } catch (DataIntegrityViolationException raceLost) {
      // 다른 chunk 가 같은 cattleNo 를 먼저 넣은 경우. 한 번만 재조회해서 그대로 쓴다.
      return repositoryCattle.findByCattleNo(data.cattleNo())
        .orElseThrow(() -> raceLost);
    }
  }

  public record ChunkPersistSummary(
    int newCattleInserted,
    int newFarmCattleLinked,
    int notFoundFromExternal,
    int failedAfterRetry
  ) {
  }
}
