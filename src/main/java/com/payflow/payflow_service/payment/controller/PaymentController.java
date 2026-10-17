package com.payflow.payflow_service.payment.controller;

import com.payflow.payflow_service.common.api.ApiResponse;
import com.payflow.payflow_service.payment.dto.CreatePaymentRequest;
import com.payflow.payflow_service.payment.dto.CreatePaymentResponse;
import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.mapper.PaymentMapper;
import com.payflow.payflow_service.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated 
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatePaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key header must not be blank") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(
                request.amount(),
                request.currency(),
                idempotencyKey);

        return ApiResponse.success(paymentMapper.toCreateResponse(payment));
    }

}