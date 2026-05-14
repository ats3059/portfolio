package com.farm.application.service;

import com.farm.application.common.ComponentExternalApiTemplate;
import com.farm.application.component.ComponentChunkPlanner;
import com.farm.application.component.ComponentFarmPushNotifier;
import com.farm.application.component.ComponentParallelCattleFetch;
import com.farm.application.domain.CattleFetchOutcome;
import com.farm.application.domain.CattleSplit;
import com.farm.application.domain.CattleState;
import com.farm.application.domain.Farm;
import com.farm.application.domain.SyncJobStatus;
import com.farm.application.external.ApiCattleTrace;
import com.farm.infrastructure.persistence.JpaRepositoryFarm;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 목장 동기화 진입점.
 *
 *  - Async 로 톰캣 워커 스레드를 즉시 풀어준다 (HTTP 응답은 곧바로 202)
 *  - 메서드 자체에는 @Transactional 이 없다. 트랜잭션은 chunk 안쪽에서만 짧게 열고 닫힌다.
 *  - 목장 인증은 1회성 이벤트이므로 SyncJob 1개를 생성하고, 그 아래 BREED / BUTCHERY chunk 를 적층한다.
 *  - push 는 각 상태의 chunk 가 모두 commit 된 뒤에 호출된다 → 레거시의 "알림 먼저, 데이터 나중" 버그를 끊어낸다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceFarmAsync {

  private final JpaRepositoryFarm repositoryFarm;
  private final ApiCattleTrace apiCattleTrace;
  private final ComponentExternalApiTemplate apiTemplate;
  private final ComponentChunkPlanner chunkPlanner;
  private final ServiceCattleFilter cattleFilter;
  private final ComponentParallelCattleFetch parallelFetch;
  private final ServiceCattlePersister cattlePersister;
  private final ServiceSyncJob serviceSyncJob;
  private final ComponentFarmPushNotifier pushNotifier;

  @Async("farmSyncExecutor")
  public void syncFarmCattle(Long farmId) {
    Farm farm = repositoryFarm.findById(farmId)
      .orElseThrow(() -> new IllegalStateException("Farm not found id=" + farmId));

    Long jobId = serviceSyncJob.createJob(farm.getId());

    try {
      int linkedAfterBreed = runStateSync(jobId, farm, CattleState.BREED);
      pushNotifier.notifyBreedSyncDone(farm, linkedAfterBreed);

      int linkedAfterButchery = runStateSync(jobId, farm, CattleState.BUTCHERY);
      pushNotifier.notifyButcherySyncDone(farm, linkedAfterButchery);
    } catch (RuntimeException ex) {
      log.error("sync aborted jobId={}, reason={}", jobId, ex.getMessage(), ex);
    } finally {
      SyncJobStatus finalStatus = serviceSyncJob.finishJob(jobId);
      log.info("job finished jobId={}, status={}", jobId, finalStatus);
    }
  }

  /**
   * 한 상태(state)에 대한 동기화 사이클.
   * 외부 호출 → chunk 계획 → chunk 별 (필터 + 병렬호출 + 짧은 트랜잭션 저장).
   *
   * @return 이번 사이클에서 새로 연결된 farm-cattle 수
   */
  private int runStateSync(Long jobId, Farm farm, CattleState state) {
    List<String> cattleNoList = apiTemplate.execute(
      "ApiCattleTrace.listFarmCattleNo",
      () -> apiCattleTrace.listFarmCattleNo(farm.getFarmNo(), state)
    );

    if (cattleNoList.isEmpty()) {
      log.info("동기화 대상 없음 farmId={}, state={}", farm.getId(), state);
      return 0;
    }

    List<List<String>> plannedChunks = chunkPlanner.plan(cattleNoList);
    List<Long> chunkIds = serviceSyncJob.persistChunks(jobId, state, plannedChunks);

    int totalLinked = 0;
    for (int idx = 0; idx < chunkIds.size(); idx++) {
      Long chunkId = chunkIds.get(idx);
      List<String> chunkCattleNos = plannedChunks.get(idx);
      try {
        totalLinked += runChunkOnce(farm, state, chunkId, chunkCattleNos);
      } catch (RuntimeException ex) {
        log.error("chunk failed jobId={}, state={}, chunkIndex={}, reason={}",
          jobId, state, idx, ex.getMessage(), ex);
        serviceSyncJob.markChunkFailed(chunkId, ex.getMessage());
        // 다음 chunk 로 진행 — 한 chunk 실패가 전체 Job 을 깨뜨리지 않게 한다
      }
    }

    return totalLinked;
  }

  /**
   * 청크 1건 처리. 청크 안의 일부 이표 실패는 recordChunkPartial 로 PARTIAL/FAILED 로 영속화.
   */
  private int runChunkOnce(Farm farm, CattleState state, Long chunkId, List<String> chunkCattleNos) {
    serviceSyncJob.markChunkInProgress(chunkId);

    CattleSplit split = cattleFilter.split(chunkCattleNos);
    CattleFetchOutcome outcome = parallelFetch.fetchAll(split.missing());

    ServiceCattlePersister.ChunkPersistSummary summary =
      cattlePersister.persistChunk(farm.getId(), state, split, outcome);

    if (outcome.failedAfterRetry().isEmpty()) {
      serviceSyncJob.markChunkDone(chunkId);
    } else {
      serviceSyncJob.recordChunkPartial(chunkId, outcome.failedAfterRetry(), "external fetch failed after retry");
    }

    log.info("chunk processed state={}, chunkId={}, summary={}, failedFetch={}",
      state, chunkId, summary, outcome.failedAfterRetry().size());
    return summary.newFarmCattleLinked();
  }
}
