package com.commerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull(message = "variantId 는 필수입니다.")
        UUID variantId,

        @NotBlank(message = "optionName 은 필수입니다.")
        String optionName,

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        int quantity,

        @Min(value = 0, message = "단가는 0원 이상이어야 합니다.")
        long unitPrice,

        @Min(value = 0, message = "할인 금액은 0원 이상이어야 합니다.")
        long discountAmount
) {
}
