package com.farm.infrastructure.persistence;

import com.farm.application.domain.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositoryFarm extends JpaRepository<Farm, Long> {
}
