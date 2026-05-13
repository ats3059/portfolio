package com.farm.application.domain;

import com.farm.application.external.CattleTraceData;
import java.util.List;
import java.util.Map;

/**
 * 병렬 외부 호출 결과 묶음.
 *
 *  - fetched      : 외부 API 응답으로 새로 받아온 개체 데이터
 *  - notFound     : 외부 시스템에서도 정보를 찾지 못한 개체번호 (정상 흐름)
 *  - failedAfterRetry : 재시도 끝에도 실패한 개체번호 (Slack 알림 발송)
 */
public record CattleFetchOutcome(
  Map<String, CattleTraceData> fetched,
  List<String> notFound,
  List<String> failedAfterRetry
) {
}
