package com.commerce.order.dto;

import com.commerce.order.domain.Order;
import com.commerce.order.domain.OrderStatus;
import java.util.UUID;

public record DataOrderResponse(
        UUID orderId,
        String orderNumber,
        String buyerName,
        long totalAmount,
        OrderStatus orderStatus
) {

    public static DataOrderResponse from(Order order) {
        return new DataOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBuyerName(),
                order.getTotalAmount(),
                order.getOrderStatus()
        );
    }
}
