package com.commerce.payment.webhook.service;

import com.commerce.global.exception.ApiBadRequestException;
import com.commerce.global.exception.ApiNotFoundException;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.dto.DataPayment;
import com.commerce.payment.repository.PaymentRepository;
import com.commerce.payment.webhook.dto.DataTossWebHook;
import com.commerce.payment.webhook.strategy.TossWebHookStatusStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class TossWebHookContext {

    private final List<TossWebHookStatusStrategy> tossWebHookStatusStrategyList;
    private final PaymentRepository paymentRepository;

    public void execute(DataTossWebHook dataTossWebHook) {
        Payment payment = paymentRepository.findByPaymentNo(dataTossWebHook.paymentKey())
                .orElseThrow(() -> new ApiNotFoundException(
                        "결제 정보를 찾을 수 없습니다. paymentKey=" + dataTossWebHook.paymentKey()));

        if (payment.getTotalAmount() != dataTossWebHook.totalAmount()) {
            throw new ApiBadRequestException(
                    "웹훅 결제 금액이 저장된 결제 금액과 일치하지 않습니다. paymentKey="
                            + dataTossWebHook.paymentKey()
                            + ", expected=" + payment.getTotalAmount()
                            + ", received=" + dataTossWebHook.totalAmount());
        }

        DataPayment dataPayment = new DataPayment(
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                dataTossWebHook.status(),
                payment.getTotalAmount()
        );

        TossWebHookStatusStrategy strategy = tossWebHookStatusStrategyList.stream()
                .filter(s -> s.supports(dataTossWebHook.status()))
                .findFirst()
                .orElseThrow(() -> new ApiBadRequestException(
                        "지원하지 않는 웹훅 상태입니다. status=" + dataTossWebHook.status()));

        strategy.updateTossStatus(dataPayment);
    }
}
