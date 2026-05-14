package com.farm.application.dto;

import java.util.List;

public record SyncJobView(
  Long jobId,
  String status,
  int totalCattle,
  String startedAt,
  String finishedAt,
  List<SyncChunkView> chunks
) {
}
