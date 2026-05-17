package com.commerce.payment.dto;

import com.commerce.payment.domain.Payment;
import com.commerce.payment.domain.PaymentMethod;
import com.commerce.payment.domain.PaymentStatus;
import java.util.UUID;

public record DataPaymentResponse(
        UUID paymentId,
        String paymentNo,
        UUID orderId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        long totalAmount
) {

    public static DataPaymentResponse from(Payment payment) {
        return new DataPaymentResponse(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTotalAmount()
        );
    }
}
