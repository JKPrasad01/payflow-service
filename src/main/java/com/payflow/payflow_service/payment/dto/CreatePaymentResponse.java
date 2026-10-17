
package com.payflow.payflow_service.payment.dto;

import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.enums.PaymentStatus;
import com.payflow.payflow_service.payment.enums.PaymentProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentProvider provider,
        String providerOrderId,
        Instant createdAt) {
    public static CreatePaymentResponse from(Payment payment) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getCreatedAt());
    }
}
