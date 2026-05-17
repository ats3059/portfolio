package com.commerce.order.service;

import com.commerce.order.domain.OrderItem;
import com.commerce.order.domain.OrderProduct;
import com.commerce.order.domain.OrderVendor;
import com.commerce.order.repository.OrderItemRepository;
import com.commerce.order.repository.OrderProductRepository;
import com.commerce.order.repository.OrderVendorRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemFinder {

    private final OrderVendorRepository orderVendorRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderItemRepository orderItemRepository;

    public List<OrderItem> findAllByOrderId(UUID orderId) {
        List<OrderVendor> vendorList = orderVendorRepository.findAllByOrderId(orderId);
        if (vendorList.isEmpty()) {
            return List.of();
        }
        List<UUID> vendorIdList = vendorList.stream().map(OrderVendor::getId).toList();

        List<OrderProduct> productList = orderProductRepository.findAllByOrderVendorIdIn(vendorIdList);
        if (productList.isEmpty()) {
            return List.of();
        }
        List<UUID> productIdList = productList.stream().map(OrderProduct::getId).toList();

        return orderItemRepository.findAllByOrderProductIdIn(productIdList);
    }
}
