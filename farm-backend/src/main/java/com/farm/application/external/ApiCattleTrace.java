package com.farm.application.external;

import com.farm.application.domain.CattleState;
import java.util.List;
import java.util.Optional;

public interface ApiCattleTrace {

  List<String> listFarmCattleNo(String farmNo, CattleState state);

  Optional<CattleTraceData> findCattleDetail(String cattleNo);
}
