package com.farm.application.dto;

public record SyncChunkView(
  Long chunkId,
  String state,
  int chunkIndex,
  String status,
  int attemptCount,
  int cattleCount,
  String lastError
) {
}
