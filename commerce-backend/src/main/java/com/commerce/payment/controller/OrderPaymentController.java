package com.commerce.payment.controller;

import com.commerce.global.response.Response;
import com.commerce.global.response.ReturnResult;
import com.commerce.payment.domain.Payment;
import com.commerce.payment.dto.DataPaymentRequest;
import com.commerce.payment.dto.DataPaymentResponse;
import com.commerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/payments")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ReturnResult<DataPaymentResponse>> startPayment(@Valid @RequestBody DataPaymentRequest request) {
        Payment payment = paymentService.startPayment(request);
        return Response.created(DataPaymentResponse.from(payment));
    }
}
