package com.commerce.payment.webhook.strategy;

import com.commerce.payment.domain.PaymentStatus;
import com.commerce.payment.dto.DataPayment;

public interface TossWebHookStatusStrategy {

    boolean supports(PaymentStatus paymentStatus);

    void updateTossStatus(DataPayment dataPayment);
}
