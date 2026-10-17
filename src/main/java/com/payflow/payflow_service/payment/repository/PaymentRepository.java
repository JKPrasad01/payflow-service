package com.payflow.payflow_service.payment.repository;

import com.payflow.payflow_service.payment.entity.Payment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            INSERT INTO payments (
                id,
                idempotency_key,
                amount,
                currency,
                status,
                provider,
                created_at,
                updated_at
            )
            VALUES (
                :id,
                :idempotencyKey,
                :amount,
                :currency,
                'CREATED',
                'RAZORPAY',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfIdempotencyKeyAbsent(
            @Param("id") UUID id,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("amount") BigDecimal amount,
            @Param("currency") String currency);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerOrderId = :id")
    Optional<Payment> findByProviderOrderIdForUpdate(@Param("id") String id);
}