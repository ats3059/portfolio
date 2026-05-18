package com.commerce.payment.webhook.strategy;

import com.commerce.global.exception.ApiNotFoundException;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.domain.PaymentStatus;
import com.commerce.payment.dto.DataPayment;
import com.commerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitingForDepositStrategy implements TossWebHookStatusStrategy {

    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(PaymentStatus paymentStatus) {
        return paymentStatus == PaymentStatus.WAITING_FOR_DEPOSIT;
    }

    @Override
    public void updateTossStatus(DataPayment dataPayment) {
        Payment payment = paymentRepository.findByPaymentNo(dataPayment.paymentNo())
                .orElseThrow(() -> new ApiNotFoundException(
                        "결제 정보를 찾을 수 없습니다. paymentNo=" + dataPayment.paymentNo()));
        payment.waitForDeposit();
    }
}
