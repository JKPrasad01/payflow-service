
package com.payflow.payflow_service.payment.service;

import com.payflow.payflow_service.common.exception.IdempotencyConflictException;
import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.provider.PaymentProviderClient;
import com.payflow.payflow_service.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProviderClient paymentProviderClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void newPaymentCreatesProviderOrder() {
        String key = "order-new";
        BigDecimal amount = new BigDecimal("250.00");
        String currency = "INR";
        UUID paymentId = UUID.randomUUID();

        Payment payment = mock(Payment.class);
        stubInsertedPayment(key, amount, currency, payment, paymentId);

        when(paymentProviderClient.createPayment(
                amount, currency, paymentId.toString()))
                .thenReturn("order_test_123");

        Payment result = paymentService.createPayment(amount, currency, key);

        assertSame(payment, result);
        verify(paymentProviderClient).createPayment(
                amount, currency, paymentId.toString());
        verify(payment).assignProviderOrderId("order_test_123");
    }

    @Test
    void repeatedRequestWithSameKeyReturnsExistingPayment() {
        String key = "order-123";
        BigDecimal amount = new BigDecimal("250.00");
        Payment payment = mock(Payment.class);

        when(payment.getAmount()).thenReturn(amount);
        when(payment.getCurrency()).thenReturn("INR");

        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(amount), eq("INR")))
                .thenReturn(0);
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment));

        assertSame(payment,
                paymentService.createPayment(amount, "INR", key));

        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void providerFailurePropagatesWithoutAssigningOrderId() {
        String key = "order-provider-failure";
        BigDecimal amount = new BigDecimal("250.00");
        UUID paymentId = UUID.randomUUID();
        Payment payment = mock(Payment.class);

        stubInsertedPayment(key, amount, "INR", payment, paymentId);

        when(paymentProviderClient.createPayment(
                amount, "INR", paymentId.toString()))
                .thenThrow(new RestClientException("Provider unavailable"));

        assertThrows(RestClientException.class,
                () -> paymentService.createPayment(amount, "INR", key));

        verify(payment, never()).assignProviderOrderId(any());
    }

    @Test
    void blankProviderOrderIdIsRejected() {
        String key = "order-empty-provider-id";
        BigDecimal amount = new BigDecimal("250.00");
        UUID paymentId = UUID.randomUUID();
        Payment payment = mock(Payment.class);

        stubInsertedPayment(key, amount, "INR", payment, paymentId);

        when(paymentProviderClient.createPayment(
                amount, "INR", paymentId.toString()))
                .thenReturn(" ");

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(amount, "INR", key));

        verify(payment, never()).assignProviderOrderId(any());
    }

    @Test
    void reusingKeyWithDifferentAmountThrowsConflict() {
        String key = "order-amount-conflict";
        BigDecimal amount = new BigDecimal("300.00");
        Payment payment = mock(Payment.class);

        when(payment.getAmount()).thenReturn(new BigDecimal("250.00"));
        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(amount), eq("INR")))
                .thenReturn(0);
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment));

        assertThrows(IdempotencyConflictException.class,
                () -> paymentService.createPayment(amount, "INR", key));

        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void reusingKeyWithDifferentCurrencyThrowsConflict() {
        String key = "order-currency-conflict";
        BigDecimal amount = new BigDecimal("250.00");
        Payment payment = mock(Payment.class);

        when(payment.getAmount()).thenReturn(amount);
        when(payment.getCurrency()).thenReturn("INR");
        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(amount), eq("USD")))
                .thenReturn(0);
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment));

        assertThrows(IdempotencyConflictException.class,
                () -> paymentService.createPayment(amount, "USD", key));

        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void missingPaymentAfterInsertThrowsIllegalStateException() {
        String key = "order-missing";
        BigDecimal amount = new BigDecimal("100.00");

        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(amount), eq("INR")))
                .thenReturn(1);
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(amount, "INR", key));

        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void sameAmountWithDifferentBigDecimalScaleIsAccepted() {
        String key = "order-scale";
        Payment payment = mock(Payment.class);

        when(payment.getAmount()).thenReturn(new BigDecimal("250.00"));
        when(payment.getCurrency()).thenReturn("INR");
        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(new BigDecimal("250.0")), eq("INR")))
                .thenReturn(0);
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment));

        assertSame(payment, paymentService.createPayment(
                new BigDecimal("250.0"), "INR", key));

        verifyNoInteractions(paymentProviderClient);
    }

    @Test
    void zeroAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        BigDecimal.ZERO, "INR", "order-zero"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void negativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("-10.00"), "INR", "order-negative"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void nullAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        null, "INR", "order-null-amount"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void nullCurrencyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), null, "order-null-currency"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void lowercaseCurrencyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), "inr", "order-lowercase"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void currencyWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), "US", "order-short-currency"));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void blankIdempotencyKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), "INR", "   "));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void nullIdempotencyKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), "INR", null));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    @Test
    void idempotencyKeyLongerThan100CharactersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(
                        new BigDecimal("100.00"), "INR", "a".repeat(101)));
        verifyNoInteractions(paymentRepository, paymentProviderClient);
    }

    private void stubInsertedPayment(
            String key,
            BigDecimal amount,
            String currency,
            Payment payment,
            UUID paymentId) {

        when(paymentRepository.insertIfIdempotencyKeyAbsent(
                any(UUID.class), eq(key), eq(amount), eq(currency)))
                .thenReturn(1);

        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(payment));

        when(payment.getAmount()).thenReturn(amount);
        when(payment.getCurrency()).thenReturn(currency);
        when(payment.getId()).thenReturn(paymentId);
    }
}
