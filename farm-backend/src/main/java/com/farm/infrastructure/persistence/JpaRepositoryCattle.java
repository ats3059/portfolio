package com.farm.infrastructure.persistence;

import com.farm.application.domain.Cattle;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositoryCattle extends JpaRepository<Cattle, Long> {

  Optional<Cattle> findByCattleNo(String cattleNo);

  List<Cattle> findByCattleNoIn(Collection<String> cattleNoList);
}
