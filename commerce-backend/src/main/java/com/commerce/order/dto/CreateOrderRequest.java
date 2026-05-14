package com.commerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "주문번호는 필수입니다.")
        String orderNumber,

        @NotBlank(message = "구매자명은 필수입니다.")
        String buyerName,

        @Min(value = 1, message = "총 결제 금액은 1원 이상이어야 합니다.")
        long totalAmount,

        @NotEmpty(message = "vendorList 는 비어있을 수 없습니다.")
        @Valid
        List<CreateOrderVendorRequest> vendorList
) {
}
