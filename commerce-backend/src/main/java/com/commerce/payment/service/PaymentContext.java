package com.commerce.payment.service;

import com.commerce.global.exception.ApiBadRequestException;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.dto.DataPayment;
import com.commerce.payment.repository.PaymentRepository;
import com.commerce.payment.strategy.PaymentStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PaymentContext {

    private final List<PaymentStrategy> paymentStrategyList;
    private final PaymentRepository paymentRepository;

    public Payment executePayment(DataPayment dataPayment) {
        Payment payment = Payment.create(
                dataPayment.paymentNo(),
                dataPayment.orderId(),
                dataPayment.paymentMethod(),
                dataPayment.totalAmount()
        );
        Payment saved = paymentRepository.save(payment);

        PaymentStrategy strategy = paymentStrategyList.stream()
                .filter(s -> s.supports(dataPayment.paymentMethod()))
                .findFirst()
                .orElseThrow(() -> new ApiBadRequestException(
                        "지원하지 않는 결제 수단입니다. paymentMethod=" + dataPayment.paymentMethod()));

        strategy.processPayment(dataPayment, saved.getId());
        return saved;
    }
}
