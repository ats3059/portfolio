package com.commerce.payment.strategy;

import com.commerce.payment.domain.PaymentMethod;
import com.commerce.payment.dto.DataPayment;
import java.util.UUID;

public interface PaymentStrategy {

    boolean supports(PaymentMethod paymentMethod);

    void processPayment(DataPayment dataPayment, UUID paymentId);
}
