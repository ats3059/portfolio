package com.commerce.order.service;

import com.commerce.global.exception.ApiConflictException;
import com.commerce.order.domain.Order;
import com.commerce.order.domain.OrderItem;
import com.commerce.order.domain.OrderProduct;
import com.commerce.order.domain.OrderVendor;
import com.commerce.order.dto.CreateOrderItemRequest;
import com.commerce.order.dto.CreateOrderProductRequest;
import com.commerce.order.dto.CreateOrderRequest;
import com.commerce.order.dto.CreateOrderVendorRequest;
import com.commerce.order.repository.OrderItemRepository;
import com.commerce.order.repository.OrderProductRepository;
import com.commerce.order.repository.OrderRepository;
import com.commerce.order.repository.OrderVendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCreateService {

    private final OrderRepository orderRepository;
    private final OrderVendorRepository orderVendorRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderItemRepository orderItemRepository;

    public Order createOrder(CreateOrderRequest request) {
        if (orderRepository.findByOrderNumber(request.orderNumber()).isPresent()) {
            throw new ApiConflictException("이미 존재하는 주문번호입니다. orderNumber=" + request.orderNumber());
        }

        Order order = orderRepository.save(
                Order.create(request.orderNumber(), request.buyerName(), request.totalAmount()));

        for (CreateOrderVendorRequest vendorRequest : request.vendorList()) {
            OrderVendor orderVendor = orderVendorRepository.save(OrderVendor.create(
                    order.getId(),
                    vendorRequest.vendorId(),
                    vendorRequest.vendorName(),
                    vendorRequest.deliveryFee()
            ));

            for (CreateOrderProductRequest productRequest : vendorRequest.productList()) {
                OrderProduct orderProduct = orderProductRepository.save(OrderProduct.create(
                        orderVendor.getId(),
                        productRequest.productId(),
                        productRequest.productName()
                ));

                for (CreateOrderItemRequest itemRequest : productRequest.itemList()) {
                    orderItemRepository.save(OrderItem.create(
                            orderProduct.getId(),
                            itemRequest.variantId(),
                            itemRequest.optionName(),
                            itemRequest.quantity(),
                            itemRequest.unitPrice(),
                            itemRequest.discountAmount()
                    ));
                }
            }
        }

        return order;
    }
}
