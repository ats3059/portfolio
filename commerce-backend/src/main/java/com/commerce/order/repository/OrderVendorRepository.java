package com.commerce.order.repository;

import com.commerce.order.domain.OrderVendor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderVendorRepository extends JpaRepository<OrderVendor, UUID> {

    List<OrderVendor> findAllByOrderId(UUID orderId);
}
