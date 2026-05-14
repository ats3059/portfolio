package com.farm.infrastructure.config;

import com.farm.application.domain.Farm;
import com.farm.infrastructure.persistence.JpaRepositoryFarm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 데모 실행을 위한 초기 데이터 주입.
 *
 * 컨트롤러 호출 시 바로 쓸 수 있도록 Farm 한 건을 부팅 시점에 박아둔다.
 * 같은 데이터가 이미 있으면 그대로 두고 넘어간다 (재실행 멱등성).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataInjector implements CommandLineRunner {

  private final JpaRepositoryFarm repositoryFarm;

  @Override
  public void run(String... args) {
    if (repositoryFarm.count() > 0) return;
    Farm seeded = repositoryFarm.save(new Farm("FARM-001", "HELLO", "010-0000-0000"));
    log.info("[init] 데모용 Farm 시드 생성 id={}, farmNo={}", seeded.getId(), seeded.getFarmNo());
  }
}
