package com.commerce.payment.service;

import com.commerce.global.exception.ApiConflictException;
import com.commerce.global.exception.ApiNotFoundException;
import com.commerce.inventory.service.StockService;
import com.commerce.order.domain.Order;
import com.commerce.order.domain.OrderItem;
import com.commerce.order.repository.OrderRepository;
import com.commerce.order.service.OrderItemFinder;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.domain.PaymentStatus;
import com.commerce.payment.dto.DataPayment;
import com.commerce.payment.dto.DataPaymentRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentContext paymentContext;
    private final OrderRepository orderRepository;
    private final OrderItemFinder orderItemFinder;
    private final StockService stockService;

    public Payment startPayment(DataPaymentRequest request) {
        Order order = orderRepository.findByOrderNumber(request.orderNumber())
                .orElseThrow(() -> new ApiNotFoundException(
                        "주문을 찾을 수 없습니다. orderNumber=" + request.orderNumber()));

        List<OrderItem> orderItemList = orderItemFinder.findAllByOrderId(order.getId());
        if (orderItemList.isEmpty()) {
            throw new ApiNotFoundException("주문 항목을 찾을 수 없습니다. orderNumber=" + request.orderNumber());
        }

        boolean stockOk = stockService.decrementStocks(orderItemList);
        if (!stockOk) {
            throw new ApiConflictException("재고가 부족하여 결제를 시작할 수 없습니다.");
        }

        DataPayment dataPayment = new DataPayment(
                request.paymentNo(),
                order.getId(),
                request.paymentMethod(),
                PaymentStatus.READY,
                order.getTotalAmount()
        );
        Payment payment = paymentContext.executePayment(dataPayment);

        order.updateOrderStatusForConfirm();

        return payment;
    }
}
