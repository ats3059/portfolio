package com.commerce.payment.webhook.dto;

import com.commerce.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DataTossWebHook(
        @NotBlank(message = "paymentKey 는 필수입니다.")
        String paymentKey,

        @NotBlank(message = "orderId 는 필수입니다.")
        String orderId,

        @NotNull(message = "status 는 필수입니다.")
        PaymentStatus status,

        long totalAmount
) {
}
