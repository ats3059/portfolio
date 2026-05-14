package com.farm.presentation;

import com.farm.application.dto.SyncJobView;
import com.farm.application.legacy.ServiceFarmCowSyncLegacy;
import com.farm.application.service.ServiceFarmAsync;
import com.farm.application.service.ServiceFarmSyncQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  - POST /farms/{farmId}/sync          : 개선판 비동기 동기화
 *  - POST /farms/{farmId}/sync/legacy   : 레거시 재현판 (비교용)
 *  - GET  /farms/{farmId}/sync/job      : Job + chunk 진행 현황 조회 (목장당 1건)
 *
 * Controller 는 Service 만 의존한다 — Repository 직접 주입 금지.
 */
@RestController
@RequestMapping("/farms/{farmId}/sync")
@RequiredArgsConstructor
public class ControllerFarmSync {

  private final ServiceFarmAsync serviceFarmAsync;
  private final ServiceFarmCowSyncLegacy serviceFarmCowSyncLegacy;
  private final ServiceFarmSyncQuery serviceFarmSyncQuery;

  @PostMapping
  public ResponseEntity<Void> syncFarm(@PathVariable Long farmId) {
    serviceFarmAsync.syncFarmCattle(farmId);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/legacy")
  public ResponseEntity<Void> syncFarmByLegacy(@PathVariable Long farmId) {
    serviceFarmCowSyncLegacy.syncFarmCattle(farmId);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/job")
  public ResponseEntity<SyncJobView> findSyncJob(@PathVariable Long farmId) {
    return serviceFarmSyncQuery.findSyncJobByFarm(farmId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
