package com.farm.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 동기화 흐름에서 사용하는 스레드 풀 두 종을 분리해 정의한다.
 *
 *  - farmSyncExecutor    : 목장 단위 비동기 진입점(org.springframework.scheduling.annotation.Async) 전용 풀
 *  - cattleFetchExecutor : chunk 내부에서 외부 API 를 병렬 호출할 때 사용하는 별도 풀
 *
 * 두 풀을 분리한 이유는 외부 API 호출 정체가 목장 단위 진입점이나 톰캣 워커 풀로 전이되지 않도록
 * 격벽을 두고, 운영 모니터링 시 큐 길이를 풀별로 따로 관측하기 위해서다.
 */
@Configuration
public class ConfigThread {

  @Bean(name = "farmSyncExecutor")
  public Executor farmSyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("farm-sync-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "cattleFetchExecutor")
  public Executor cattleFetchExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("cattle-fetch-");
    executor.initialize();
    return executor;
  }
}
