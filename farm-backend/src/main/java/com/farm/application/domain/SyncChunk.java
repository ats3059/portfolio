package com.farm.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 같은 Job 아래에서 상태(BREED/BUTCHERY) 와 chunkIndex 로 구분되는 청크 단위.
 *
 *  - 한 chunk 는 외부 API 호출 결과 반영 + DB 저장까지 하나의 짧은 트랜잭션으로 종료된다
 *  - 부분 실패 시 해당 chunk 만 PARTIAL/FAILED 로 남고 인접 chunk 의 정상 커밋은 유지된다
 *  - 본문에 cattleNos 를 보존해 동일 입력으로 재처리가 가능하다
 *  - 청크 안에서 일부 이표만 실패한 경우 failedCattleNos 에 콤마 구분으로 보관된다
 */
@Entity
@Getter
@Table(name = "sync_chunk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncChunk {

  private static final String DELIMITER = ",";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long jobId;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private CattleState state;

  @Column(nullable = false)
  private int chunkIndex;

  @Lob
  @Column(nullable = false)
  private String cattleNosCsv;

  @Lob
  private String failedCattleNosCsv;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SyncChunkStatus status;

  @Column(nullable = false)
  private int attemptCount;

  @Column(length = 500)
  private String lastError;

  private LocalDateTime processedAt;

  public SyncChunk(Long jobId, CattleState state, int chunkIndex, List<String> cattleNos) {
    this.jobId = jobId;
    this.state = state;
    this.chunkIndex = chunkIndex;
    this.cattleNosCsv = String.join(DELIMITER, cattleNos);
    this.status = SyncChunkStatus.PENDING;
    this.attemptCount = 0;
  }

  public List<String> cattleNos() {
    if (cattleNosCsv == null || cattleNosCsv.isBlank()) return List.of();
    return Arrays.asList(cattleNosCsv.split(DELIMITER));
  }

  public List<String> failedCattleNos() {
    if (failedCattleNosCsv == null || failedCattleNosCsv.isBlank()) return List.of();
    return Arrays.asList(failedCattleNosCsv.split(DELIMITER));
  }

  public void markInProgress() {
    this.status = SyncChunkStatus.IN_PROGRESS;
    this.attemptCount += 1;
  }

  public void markDone() {
    this.status = SyncChunkStatus.DONE;
    this.lastError = null;
    this.failedCattleNosCsv = null;
    this.processedAt = LocalDateTime.now();
  }

  public void markFailed(String reason) {
    this.status = SyncChunkStatus.FAILED;
    this.lastError = truncate(reason);
    this.processedAt = LocalDateTime.now();
  }

  /**
   * 청크 안에서 일부 이표만 실패한 경우. failedCattleNos 가 비어있으면 DONE, 전부 실패면 FAILED, 아니면 PARTIAL.
   */
  public void recordPartial(List<String> failedCattleNos, String firstError) {
    if (failedCattleNos == null || failedCattleNos.isEmpty()) {
      markDone();
      return;
    }
    if (failedCattleNos.size() >= cattleNos().size()) {
      this.status = SyncChunkStatus.FAILED;
    } else {
      this.status = SyncChunkStatus.PARTIAL;
    }
    this.failedCattleNosCsv = String.join(DELIMITER, failedCattleNos);
    this.lastError = truncate(firstError);
    this.processedAt = LocalDateTime.now();
  }

  private static String truncate(String reason) {
    if (reason == null) return "unknown";
    return reason.length() <= 500 ? reason : reason.substring(0, 500);
  }
}
