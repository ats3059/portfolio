package com.commerce.payment.strategy;

import com.commerce.payment.domain.PaymentMethod;
import com.commerce.payment.dto.DataPayment;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VirtualAccountPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.VIRTUAL_ACCOUNT;
    }

    @Override
    public void processPayment(DataPayment dataPayment, UUID paymentId) {
    }
}
