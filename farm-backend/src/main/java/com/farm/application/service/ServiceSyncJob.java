package com.farm.application.service;

import com.farm.application.domain.CattleState;
import com.farm.application.domain.SyncChunk;
import com.farm.application.domain.SyncChunkStatus;
import com.farm.application.domain.SyncJob;
import com.farm.application.domain.SyncJobStatus;
import com.farm.infrastructure.persistence.JpaRepositorySyncChunk;
import com.farm.infrastructure.persistence.JpaRepositorySyncJob;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SyncJob / SyncChunk 라이프사이클을 짧은 트랜잭션 단위로 분리해 관리한다.
 *
 * 모든 메서드가 Propagation.REQUIRES_NEW 로 선언되어 있어 외부 API 호출이나
 * push 발송 같은 느린 작업과 동일 트랜잭션에 묶이지 않으며, 단일 chunk 의 실패가
 * 인접 chunk 의 정상 커밋을 가두지 않는다.
 *
 * 목장 인증은 도메인 상 1회 발생 이벤트이므로 Job 은 farmId 에 대해 유일하다.
 * 동일 farmId 에 대한 재호출은 IllegalStateException 으로 차단된다.
 */
@Service
@RequiredArgsConstructor
public class ServiceSyncJob {

  private final JpaRepositorySyncJob repositoryJob;
  private final JpaRepositorySyncChunk repositoryChunk;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Long createJob(Long farmId) {
    repositoryJob.findByFarmId(farmId).ifPresent(existing -> {
      throw new IllegalStateException("이미 동기화된 목장입니다 farmId=" + farmId);
    });
    SyncJob job = repositoryJob.save(new SyncJob(farmId));
    return job.getId();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<Long> persistChunks(Long jobId, CattleState state, List<List<String>> chunks) {
    List<Long> chunkIds = new ArrayList<>(chunks.size());
    for (int idx = 0; idx < chunks.size(); idx++) {
      SyncChunk saved = repositoryChunk.save(new SyncChunk(jobId, state, idx, chunks.get(idx)));
      chunkIds.add(saved.getId());
    }
    return chunkIds;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markChunkInProgress(Long chunkId) {
    SyncChunk chunk = repositoryChunk.findById(chunkId).orElseThrow();
    chunk.markInProgress();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markChunkDone(Long chunkId) {
    SyncChunk chunk = repositoryChunk.findById(chunkId).orElseThrow();
    chunk.markDone();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markChunkFailed(Long chunkId, String reason) {
    SyncChunk chunk = repositoryChunk.findById(chunkId).orElseThrow();
    chunk.markFailed(reason);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordChunkPartial(Long chunkId, List<String> failedCattleNos, String firstError) {
    SyncChunk chunk = repositoryChunk.findById(chunkId).orElseThrow();
    chunk.recordPartial(failedCattleNos, firstError);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SyncJobStatus finishJob(Long jobId) {
    SyncJob job = repositoryJob.findById(jobId).orElseThrow();
    long failed = repositoryChunk.countByJobIdAndStatus(jobId, SyncChunkStatus.FAILED);
    long partial = repositoryChunk.countByJobIdAndStatus(jobId, SyncChunkStatus.PARTIAL);
    SyncJobStatus finalStatus = (failed == 0 && partial == 0) ? SyncJobStatus.COMPLETED : SyncJobStatus.PARTIALLY_FAILED;
    job.finish(finalStatus);
    return finalStatus;
  }
}
