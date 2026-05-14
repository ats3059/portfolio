package com.commerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_number", columnList = "orderNumber", unique = true)
        }
)
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    public static Order create(String orderNumber, String buyerName, long totalAmount) {
        Order order = new Order();
        order.orderNumber = orderNumber;
        order.buyerName = buyerName;
        order.totalAmount = totalAmount;
        order.orderStatus = OrderStatus.PENDING_CONFIRM;
        return order;
    }

    public void updateOrderStatusForConfirm() {
        if (orderStatus == OrderStatus.CONFIRM) {
            return;
        }
        if (orderStatus != OrderStatus.PENDING_CONFIRM) {
            throw new IllegalStateException("주문 확정은 PENDING_CONFIRM 상태에서만 가능합니다. 현재 상태: " + orderStatus);
        }
        this.orderStatus = OrderStatus.CONFIRM;
    }
}
