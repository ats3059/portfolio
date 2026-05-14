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
        name = "order_item",
        indexes = {
                @Index(name = "idx_order_item_order_product_id", columnList = "orderProductId"),
                @Index(name = "idx_order_item_variant_id", columnList = "variantId")
        }
)
public class OrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_product_id", nullable = false)
    private UUID orderProductId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemStatus status;

    public static OrderItem create(UUID orderProductId, UUID variantId, String optionName,
                                   int quantity, long unitPrice, long discountAmount) {
        OrderItem orderItem = new OrderItem();
        orderItem.orderProductId = orderProductId;
        orderItem.variantId = variantId;
        orderItem.optionName = optionName;
        orderItem.quantity = quantity;
        orderItem.unitPrice = unitPrice;
        orderItem.discountAmount = discountAmount;
        orderItem.status = OrderItemStatus.PAYMENT_PENDING;
        return orderItem;
    }

    public void paymentCompleted() {
        if (status == OrderItemStatus.PAYMENT_COMPLETED) {
            return;
        }
        if (status != OrderItemStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 완료 전이는 PAYMENT_PENDING 상태에서만 가능합니다. 현재 상태: " + status);
        }
        this.status = OrderItemStatus.PAYMENT_COMPLETED;
    }

    public void paymentCanceledConfirm() {
        if (status == OrderItemStatus.PAYMENT_CANCELED_CONFIRM) {
            return;
        }
        this.status = OrderItemStatus.PAYMENT_CANCELED_CONFIRM;
    }

    public void abort() {
        if (status == OrderItemStatus.ABORTED) {
            return;
        }
        this.status = OrderItemStatus.ABORTED;
    }

    public void changeStatus(OrderItemStatus status) {
        this.status = status;
    }
}
