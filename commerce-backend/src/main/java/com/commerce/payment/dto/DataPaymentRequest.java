package com.commerce.payment.dto;

import com.commerce.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DataPaymentRequest(
        @NotBlank(message = "주문번호는 필수입니다.")
        String orderNumber,

        @NotBlank(message = "결제번호는 필수입니다.")
        String paymentNo,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod paymentMethod
) {
}
