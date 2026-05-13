package com.farm.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 목장 인증은 도메인 상 1회 발생 이벤트이므로 Job 은 farmId 에 대해 유일하게 존재한다.
 * BREED 와 BUTCHERY 두 상태는 동일 Job 아래 SyncChunk 가 state 컬럼으로 구분해 적층된다.
 */
@Entity
@Getter
@Table(name = "sync_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long farmId;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SyncJobStatus status;

  @Column(nullable = false)
  private LocalDateTime startedAt;

  private LocalDateTime finishedAt;

  public SyncJob(Long farmId) {
    this.farmId = farmId;
    this.status = SyncJobStatus.RUNNING;
    this.startedAt = LocalDateTime.now();
  }

  public void finish(SyncJobStatus finalStatus) {
    this.status = finalStatus;
    this.finishedAt = LocalDateTime.now();
  }
}
