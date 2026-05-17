package com.commerce.inventory.repository;

import com.commerce.inventory.domain.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pv from ProductVariant pv where pv.id in :variantIdList")
    List<ProductVariant> findAllByIdInForUpdate(@Param("variantIdList") List<UUID> variantIdList);
}
