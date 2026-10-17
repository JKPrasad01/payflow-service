
package com.payflow.payflow_service.payment.service;

import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.enums.PaymentStatus;
import com.payflow.payflow_service.payment.provider.RazorpayWebhookVerifier;
import com.payflow.payflow_service.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentWebhookService {

    private final PaymentRepository paymentRepository;
    private final RazorpayWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public PaymentWebhookService(
            PaymentRepository paymentRepository,
            RazorpayWebhookVerifier webhookVerifier,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(String rawBody, String signature) {
        if (!webhookVerifier.isValidSignature(rawBody, signature)) {
            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();

            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");

            String providerPaymentId = paymentEntity.path("id").asText(null);
            String providerOrderId = paymentEntity.path("order_id").asText(null);
            String providerStatus = paymentEntity.path("status").asText(null);

            if (providerOrderId == null || providerOrderId.isBlank()) {
                throw new IllegalArgumentException(
                        "Webhook is missing the provider order ID");
            }

            Payment payment = paymentRepository
                    .findByProviderOrderId(providerOrderId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment not found for provider order ID"));

            switch (event) {
                case "payment.authorized" -> {
                    if (payment.getStatus() == PaymentStatus.CREATED) {
                        if (providerPaymentId == null || providerPaymentId.isBlank()) {
                            throw new IllegalArgumentException(
                                    "Webhook is missing the provider payment ID");
                        }
                        payment.markPending(providerPaymentId);
                    }
                }

                case "payment.captured" -> {
                    if (payment.getStatus() == PaymentStatus.PENDING) {
                        payment.markSucceeded();
                    } else if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                        throw new IllegalStateException(
                                "Payment cannot be marked successful from its current state");
                    }
                }

                case "payment.failed" -> {
                    if (payment.getStatus() == PaymentStatus.PENDING) {
                        payment.markFailed();
                    } else if (payment.getStatus() != PaymentStatus.FAILED) {
                        throw new IllegalStateException(
                                "Payment cannot be marked failed from its current state");
                    }
                }

                default -> {
                    // Ignore unrelated event types.
                }
            }

        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook payload", exception);
        }
    }
}
