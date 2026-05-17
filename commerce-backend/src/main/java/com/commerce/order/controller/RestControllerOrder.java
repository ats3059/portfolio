package com.commerce.order.controller;

import com.commerce.global.response.Response;
import com.commerce.global.response.ReturnResult;
import com.commerce.order.domain.Order;
import com.commerce.order.dto.CreateOrderRequest;
import com.commerce.order.dto.DataOrderResponse;
import com.commerce.order.service.OrderCreateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RestControllerOrder {

    private final OrderCreateService orderCreateService;

    @PostMapping
    public ResponseEntity<ReturnResult<DataOrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderCreateService.createOrder(request);
        return Response.created(DataOrderResponse.from(order));
    }
}
