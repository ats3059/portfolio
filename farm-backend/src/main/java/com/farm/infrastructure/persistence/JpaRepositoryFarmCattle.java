package com.farm.infrastructure.persistence;

import com.farm.application.domain.Cattle;
import com.farm.application.domain.Farm;
import com.farm.application.domain.FarmCattle;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRepositoryFarmCattle extends JpaRepository<FarmCattle, Long> {

  /**
   * 레거시 재현용 — chunk 안에서 한 마리씩 호출되며 N+1 SELECT 를 발생시킨다.
   * 개선판은 {@link #findLinkedCattleIds(Long, Collection)} 를 사용한다.
   */
  long countByFarmAndCattle(Farm farm, Cattle cattle);

  /**
   * 주어진 farm 에 이미 연결된 cattleId 만 한 번에 조회한다.
   * chunk size 만큼 단건 countByFarmAndCattle 을 돌리던 N+1 패턴을 단일 IN 쿼리로 대체.
   */
  @Query("select fc.cattle.id from FarmCattle fc where fc.farm.id = :farmId and fc.cattle.id in :cattleIds")
  Set<Long> findLinkedCattleIds(@Param("farmId") Long farmId, @Param("cattleIds") Collection<Long> cattleIds);
}
