
package com.payflow.payflow_service.payment.service;

import com.payflow.payflow_service.common.exception.IdempotencyConflictException;
import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.provider.PaymentProviderClient;
import com.payflow.payflow_service.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderClient paymentProviderClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProviderClient paymentProviderClient) {
        this.paymentRepository = paymentRepository;
        this.paymentProviderClient = paymentProviderClient;
    }

    @Transactional
    public Payment createPayment(
            BigDecimal amount,
            String currency,
            String idempotencyKey) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must be a 3-letter uppercase code, such as INR");
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency key must contain 1 to 100 characters");
        }

        UUID paymentId = UUID.randomUUID();

        int inserted = paymentRepository.insertIfIdempotencyKeyAbsent(
                paymentId,
                idempotencyKey,
                amount,
                currency);

        Payment payment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment was not found after idempotent insert"));

        if (payment.getAmount().compareTo(amount) != 0
                || !payment.getCurrency().equals(currency)) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with different payment details");
        }

        // A retry must not create another provider order.
        if (inserted == 0) {
            return payment;
        }

        String providerOrderId = paymentProviderClient.createPayment(
                amount,
                currency,
                payment.getId().toString());

        if (providerOrderId == null || providerOrderId.isBlank()) {
            throw new IllegalStateException(
                    "Payment provider returned an empty order ID");
        }

        payment.assignProviderOrderId(providerOrderId);

        return payment;
    }
}
