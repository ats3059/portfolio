package com.farm.infrastructure.persistence;

import com.farm.application.domain.SyncChunk;
import com.farm.application.domain.SyncChunkStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositorySyncChunk extends JpaRepository<SyncChunk, Long> {

  List<SyncChunk> findByJobIdOrderByChunkIndexAsc(Long jobId);

  long countByJobIdAndStatus(Long jobId, SyncChunkStatus status);
}
