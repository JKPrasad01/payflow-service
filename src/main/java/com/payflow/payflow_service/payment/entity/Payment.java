package com.payflow.payflow_service.payment.entity;

import com.payflow.payflow_service.payment.enums.PaymentProvider;
import com.payflow.payflow_service.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_order_id", length = 100)
    private String providerOrderId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Payment(
            BigDecimal amount,
            String currency,
            PaymentProvider provider,
            String idempotencyKey) {
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.CREATED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markPending(String providerPaymentId) {
        if (this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a CREATED payment can transition to PENDING");
        }

        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider payment ID must not be blank");
        }

        this.providerPaymentId = providerPaymentId;
        this.status = PaymentStatus.PENDING;
    }

    public void markSucceeded(String providerPaymentId) {
        if (status == PaymentStatus.SUCCEEDED)
            return; // idempotent
        if (status != PaymentStatus.CREATED && status != PaymentStatus.PENDING)
            throw new IllegalStateException("Cannot succeed from " + status);
        this.providerPaymentId = providerPaymentId;
        this.status = PaymentStatus.SUCCEEDED;
    }

    public void markFailed() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a PENDING payment can transition to FAILED");
        }

        this.status = PaymentStatus.FAILED;
    }

    public void markCancelled() {
        if (this.status != PaymentStatus.CREATED
                && this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a CREATED or PENDING payment can be cancelled");
        }

        this.status = PaymentStatus.CANCELLED;
    }

    public void assignProviderOrderId(String providerOrderId) {
        if (this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                    "A provider order can only be assigned to a CREATED payment");
        }

        if (providerOrderId == null || providerOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider order ID must not be blank");
        }

        if (this.providerOrderId != null) {
            if (this.providerOrderId.equals(providerOrderId)) {
                return;
            }

            throw new IllegalStateException(
                    "A different provider order ID is already assigned");
        }

        this.providerOrderId = providerOrderId;
    }
}