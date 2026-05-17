package com.commerce.payment.strategy;

import com.commerce.global.exception.ApiNotFoundException;
import com.commerce.order.domain.OrderItem;
import com.commerce.order.service.OrderItemFinder;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.domain.PaymentMethod;
import com.commerce.payment.dto.DataPayment;
import com.commerce.payment.repository.PaymentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final OrderItemFinder orderItemFinder;

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CARD;
    }

    @Override
    public void processPayment(DataPayment dataPayment, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiNotFoundException("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));
        payment.confirm();

        List<OrderItem> orderItemList = orderItemFinder.findAllByOrderId(payment.getOrderId());
        for (OrderItem orderItem : orderItemList) {
            orderItem.paymentCompleted();
        }
    }
}
