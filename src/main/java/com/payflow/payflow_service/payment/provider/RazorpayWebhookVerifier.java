
package com.payflow.payflow_service.payment.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayWebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;

    public RazorpayWebhookVerifier(
            @Value("${razorpay.webhook-secret}") String webhookSecret) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay webhook secret must be configured");
        }

        this.webhookSecret = webhookSecret;
    }

    public boolean isValidSignature(String rawBody, String signature) {
        if (rawBody == null || signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256));

            byte[] expectedSignature = mac.doFinal(
                    rawBody.getBytes(StandardCharsets.UTF_8));

            byte[] providedSignature = HexFormat.of().parseHex(signature);

            return MessageDigest.isEqual(
                    expectedSignature,
                    providedSignature);

        } catch (java.security.GeneralSecurityException
                | IllegalArgumentException exception) {
            return false;
        }
    }
}
