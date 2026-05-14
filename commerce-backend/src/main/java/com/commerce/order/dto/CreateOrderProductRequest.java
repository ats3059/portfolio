package com.commerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateOrderProductRequest(
        @NotNull(message = "productId 는 필수입니다.")
        UUID productId,

        @NotBlank(message = "productName 은 필수입니다.")
        String productName,

        @NotEmpty(message = "itemList 는 비어있을 수 없습니다.")
        @Valid
        List<CreateOrderItemRequest> itemList
) {
}
