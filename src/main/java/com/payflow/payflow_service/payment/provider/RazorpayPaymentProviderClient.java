
package com.payflow.payflow_service.payment.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RazorpayPaymentProviderClient implements PaymentProviderClient {

    private final RestClient restClient;

    public RazorpayPaymentProviderClient(
            RestClient.Builder restClientBuilder,
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret) {

        this.restClient = restClientBuilder
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public String createPayment(
            BigDecimal amount,
            String currency,
            String internalPaymentId) {

        if (!"INR".equals(currency)) {
            throw new IllegalArgumentException(
                    "Only INR is supported by this Razorpay adapter");
        }

        long amountInPaise = amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        Map<String, Object> request = Map.of(
                "amount", amountInPaise,
                "currency", currency,
                "receipt", internalPaymentId);

        RazorpayOrderResponse response = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RazorpayOrderResponse.class);

        if (response == null || response.id() == null
                || response.id().isBlank()) {
            throw new IllegalStateException(
                    "Razorpay returned an invalid order response");
        }

        return response.id();
    }

    private record RazorpayOrderResponse(String id) {
    }
}
