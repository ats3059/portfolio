package com.farm.application.component;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 한 번에 받아온 개체 번호 리스트를 일정 크기 chunk 로 잘라준다.
 * 레거시는 1000마리짜리 목장도 단일 트랜잭션 안에서 통째로 돌렸지만,
 * 여기서는 Planner 가 잘라낸 chunk 단위가 곧 트랜잭션 단위가 된다.
 *
 * <p>기본 청크 크기는 {@code farm.sync.chunk-size} 설정으로 외부화돼 있다 (기본값 100).
 * 운영 중 청크 크기를 조정하려면 설정만 바꾸고 재배포하면 된다.
 */
@Component
public class ComponentChunkPlanner {

  private final int defaultChunkSize;

  public ComponentChunkPlanner(@Value("${farm.sync.chunk-size:100}") int defaultChunkSize) {
    if (defaultChunkSize <= 0) {
      throw new IllegalArgumentException("farm.sync.chunk-size must be positive but was " + defaultChunkSize);
    }
    this.defaultChunkSize = defaultChunkSize;
  }

  public List<List<String>> plan(List<String> cattleNoList) {
    return plan(cattleNoList, defaultChunkSize);
  }

  public List<List<String>> plan(List<String> cattleNoList, int chunkSize) {
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize must be positive but was " + chunkSize);
    }
    if (cattleNoList.isEmpty()) return List.of();

    int total = cattleNoList.size();
    List<List<String>> chunks = new ArrayList<>((total + chunkSize - 1) / chunkSize);
    for (int from = 0; from < total; from += chunkSize) {
      int to = Math.min(from + chunkSize, total);
      chunks.add(List.copyOf(cattleNoList.subList(from, to)));
    }
    return chunks;
  }
}
