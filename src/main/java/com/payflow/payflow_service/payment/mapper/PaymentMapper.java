
package com.payflow.payflow_service.payment.mapper;

import org.springframework.stereotype.Component;

import com.payflow.payflow_service.payment.dto.CreatePaymentResponse;
import com.payflow.payflow_service.payment.entity.Payment;

@Component
public class PaymentMapper {

    public CreatePaymentResponse toCreateResponse(Payment payment) {
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
