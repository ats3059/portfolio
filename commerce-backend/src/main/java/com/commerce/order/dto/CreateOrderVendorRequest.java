package com.commerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateOrderVendorRequest(
        @NotNull(message = "vendorId 는 필수입니다.")
        UUID vendorId,

        @NotBlank(message = "vendorName 은 필수입니다.")
        String vendorName,

        @Min(value = 0, message = "배송비는 0원 이상이어야 합니다.")
        long deliveryFee,

        @NotEmpty(message = "productList 는 비어있을 수 없습니다.")
        @Valid
        List<CreateOrderProductRequest> productList
) {
}
