package com.farm.application.legacy;

import com.farm.application.domain.Cattle;
import com.farm.application.domain.CattleState;
import com.farm.application.domain.Farm;
import com.farm.application.domain.FarmCattle;
import com.farm.infrastructure.persistence.JpaRepositoryCattle;
import com.farm.infrastructure.persistence.JpaRepositoryFarm;
import com.farm.infrastructure.persistence.JpaRepositoryFarmCattle;
import com.farm.application.external.ApiCattleTrace;
import com.farm.application.external.CattleTraceData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의도적으로 안티패턴을 보존한 레거시 재현 클래스.
 *
 * <p>단일 {@code @Transactional} + 직렬 외부 API 호출 + 트랜잭션 안 push + N+1 SELECT 등
 * 8가지 구조적 문제를 의도해 남겼다. 각 문제는 본문에 {@code [문제 N]} 주석으로 표시.
 *
 * <p>개선판 {@code ServiceFarmAsync} 와 같은 데이터셋에서 비교하려고 남긴 코드.
 * {@code POST /farms/{farmId}/sync/legacy} 로 직접 실행할 수 있다.
 */
@Service
public class ServiceFarmCowSyncLegacy {

  @Autowired
  private JpaRepositoryFarm repositoryFarm;

  @Autowired
  private JpaRepositoryCattle repositoryCattle;

  @Autowired
  private JpaRepositoryFarmCattle repositoryFarmCattle;

  @Autowired
  private ApiCattleTrace apiCattleTrace;

  /**
   * [문제 1] 사육 + 도축 + 외부 API + DB 저장을 전부 하나의 @Transactional 안에서 처리.
   * [문제 2] 트랜잭션 내부에서 외부 API 를 호출하므로 tx 구간이 수 분 단위로 벌어지고
   *         FK 무결성 검사를 위한 S-Lock 이 그 시간만큼 유지돼 목장 수정이 Lock Timeout.
   */
  @Async
  @Transactional
  public void syncFarmCattle(Long farmId) {
    Farm farm = repositoryFarm.findById(farmId).orElseThrow();

    syncByState(farm, CattleState.BREED);

    // [문제 3] Push 를 트랜잭션 커밋 전에 발송.
    //         사용자 앱에는 방금 받은 알림 기준으로 들어왔지만 DB 커밋은 아직 안 됐음.
    //         -> "알림은 왔는데 목록이 안 보여요" 문의 발생.
    sendPushNaively(farm);

    syncByState(farm, CattleState.BUTCHERY);
  }

  private void syncByState(Farm farm, CattleState state) {
    List<String> cattleNoList = listCattleNoWithSleepRetry(farm, state);
    if (cattleNoList.isEmpty()) return;

    List<String> failedList = new ArrayList<>();

    for (String cattleNo : cattleNoList) {
      try {
        // [문제 4] N+1 조회: 개체 하나마다 findByCattleNo 를 호출한다.
        //         목장이 1000마리면 최소 1000 번의 SELECT 가 트랜잭션 안에서 일어난다.
        Cattle cattle = repositoryCattle.findByCattleNo(cattleNo).orElse(null);

        if (cattle == null) {
          // [문제 5] 외부 API 를 개체별로 직렬 호출. 병렬 처리 없음, RateLimiter 없음.
          //         이미 DB 에 존재하는 개체에 대해서도 조건에 따라 호출되는 경우가 많아
          //         실제 1000마리 중 800마리가 이미 있어도 대부분 외부 호출이 발생했다.
          Optional<CattleTraceData> detail = apiCattleTrace.findCattleDetail(cattleNo);
          if (detail.isEmpty()) {
            failedList.add(cattleNo);
            continue;
          }
          CattleTraceData data = detail.get();
          cattle = repositoryCattle.save(new Cattle(data.cattleNo(), data.gender(), data.birthDate()));
        }

        if (repositoryFarmCattle.countByFarmAndCattle(farm, cattle) > 0) continue;

        repositoryFarmCattle.save(new FarmCattle(farm, cattle, state));
      } catch (Exception e) {
        // [문제 6] 실패한 개체 번호를 List 에 담아두기만 한다. 부분 재시도 수단이 없다.
        failedList.add(cattleNo);
      }
    }

    if (!failedList.isEmpty()) {
      // [문제 7] 실패를 표준 출력으로만 찍고 Slack 같은 알림 수단이 없어
      //         운영팀이 외부 API 장애인지 서버 문제인지 즉시 구분할 수 없다.
      System.out.println("[legacy] " + state + " 동기화 실패 개체 수 = " + failedList.size());
    }
  }

  /**
   * [문제 8] 외부 API 일시 실패 시 Thread.sleep 으로 블로킹 재시도.
   *         트랜잭션을 물고 있는 상태에서 sleep 하므로 Lock 도 같이 물린 채 잠든다.
   */
  private List<String> listCattleNoWithSleepRetry(Farm farm, CattleState state) {
    for (int tryCnt = 0; tryCnt < 3; tryCnt++) {
      try {
        List<String> list = apiCattleTrace.listFarmCattleNo(farm.getFarmNo(), state);
        if (!list.isEmpty()) return list;
      } catch (Exception ignored) {
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return List.of();
  }

  private void sendPushNaively(Farm farm) {
    // 실제 환경에서는 외부 push service 를 호출하는 지점.
    System.out.println("[legacy] Push 발송: " + farm.getFarmerName() + " 목장 동기화가 곧 완료됩니다.");
  }
}
