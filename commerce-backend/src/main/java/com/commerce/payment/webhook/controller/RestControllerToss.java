package com.commerce.payment.webhook.controller;

import com.commerce.global.response.Response;
import com.commerce.global.response.ReturnResult;
import com.commerce.payment.webhook.dto.DataTossWebHook;
import com.commerce.payment.webhook.service.TossWebHookContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payment/toss/webhook")
@RequiredArgsConstructor
public class RestControllerToss {

    private final TossWebHookContext tossWebHookContext;

    @PostMapping("/payment-status-changed")
    public ResponseEntity<ReturnResult<Void>> handleStatusChanged(@Valid @RequestBody DataTossWebHook dataTossWebHook) {
        tossWebHookContext.execute(dataTossWebHook);
        return Response.ok();
    }
}
