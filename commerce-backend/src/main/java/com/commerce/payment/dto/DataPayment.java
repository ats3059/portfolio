package com.commerce.payment.dto;

import com.commerce.payment.domain.Payment;
import com.commerce.payment.domain.PaymentMethod;
import com.commerce.payment.domain.PaymentStatus;
import java.util.UUID;

public record DataPayment(
        String paymentNo,
        UUID orderId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        long totalAmount
) {

    public static DataPayment from(Payment payment) {
        return new DataPayment(
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTotalAmount()
        );
    }
}
