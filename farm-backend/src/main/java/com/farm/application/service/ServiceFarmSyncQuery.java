package com.farm.application.service;

import com.farm.application.domain.SyncChunk;
import com.farm.application.domain.SyncJob;
import com.farm.application.dto.SyncChunkView;
import com.farm.application.dto.SyncJobView;
import com.farm.infrastructure.persistence.JpaRepositorySyncChunk;
import com.farm.infrastructure.persistence.JpaRepositorySyncJob;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동기화 진행 현황 조회 전용 서비스.
 *
 * 한 목장은 1회성 인증으로 단일 SyncJob 만 가지므로 단수 조회를 노출한다.
 * Controller 가 Repository 를 직접 의존하지 않도록 read-only 흐름은 이 서비스로 모은다.
 */
@Service
@RequiredArgsConstructor
public class ServiceFarmSyncQuery {

  private final JpaRepositorySyncJob repositoryJob;
  private final JpaRepositorySyncChunk repositoryChunk;

  @Transactional(readOnly = true)
  public Optional<SyncJobView> findSyncJobByFarm(Long farmId) {
    return repositoryJob.findByFarmId(farmId).map(this::toView);
  }

  private SyncJobView toView(SyncJob job) {
    List<SyncChunk> chunks = repositoryChunk.findByJobIdOrderByChunkIndexAsc(job.getId());
    int totalCattle = chunks.stream().mapToInt(c -> c.cattleNos().size()).sum();
    List<SyncChunkView> chunkViews = chunks.stream().map(this::toView).toList();
    return new SyncJobView(
      job.getId(),
      job.getStatus().name(),
      totalCattle,
      job.getStartedAt() == null ? null : job.getStartedAt().toString(),
      job.getFinishedAt() == null ? null : job.getFinishedAt().toString(),
      chunkViews
    );
  }

  private SyncChunkView toView(SyncChunk chunk) {
    return new SyncChunkView(
      chunk.getId(),
      chunk.getState().name(),
      chunk.getChunkIndex(),
      chunk.getStatus().name(),
      chunk.getAttemptCount(),
      chunk.cattleNos().size(),
      chunk.getLastError()
    );
  }
}
