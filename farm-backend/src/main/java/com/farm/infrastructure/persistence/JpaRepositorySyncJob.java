package com.farm.infrastructure.persistence;

import com.farm.application.domain.SyncJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositorySyncJob extends JpaRepository<SyncJob, Long> {

  Optional<SyncJob> findByFarmId(Long farmId);
}
