package com.commerce.inventory.service;

import com.commerce.global.exception.ApiNotFoundException;
import com.commerce.inventory.domain.ProductVariant;
import com.commerce.inventory.repository.ProductVariantRepository;
import com.commerce.order.domain.OrderItem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final ProductVariantRepository productVariantRepository;

    public boolean decrementStocks(List<OrderItem> orderItemList) {
        List<UUID> variantIdList = orderItemList.stream()
                .map(OrderItem::getVariantId)
                .toList();

        List<ProductVariant> variantList = productVariantRepository.findAllByIdInForUpdate(variantIdList);
        Map<UUID, ProductVariant> variantMap = variantList.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (OrderItem orderItem : orderItemList) {
            ProductVariant variant = variantMap.get(orderItem.getVariantId());
            if (variant == null) {
                throw new ApiNotFoundException("재고 정보를 찾을 수 없습니다. variantId=" + orderItem.getVariantId());
            }
            if (!variant.decrementStock(orderItem.getQuantity())) {
                return false;
            }
        }
        return true;
    }
}
