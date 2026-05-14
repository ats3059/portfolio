package com.commerce.order.repository;

import com.commerce.order.domain.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderProductIdIn(List<UUID> orderProductIdList);
}
