package com.commerce.order.repository;

import com.commerce.order.domain.OrderProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, UUID> {

    List<OrderProduct> findAllByOrderVendorIdIn(List<UUID> orderVendorIdList);
}
