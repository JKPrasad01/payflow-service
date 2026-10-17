
package com.payflow.payflow_service.payment.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class RazorpayPaymentProviderClientTest {

    private MockRestServiceServer mockServer;
    private RazorpayPaymentProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(builder).build();

        client = new RazorpayPaymentProviderClient(
                builder,
                "test_key_id",
                "test_key_secret");
    }

    @Test
    void createPayment_sendsCorrectOrderRequest() {
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString(
                "test_key_id:test_key_secret".getBytes(StandardCharsets.UTF_8));

        mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, expectedAuth))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "amount": 25000,
                          "currency": "INR",
                          "receipt": "payment-123"
                        }
                        """))
                .andRespond(withSuccess(
                        """
                                {"id":"order_test_123"}
                                """,
                        MediaType.APPLICATION_JSON));

        String orderId = client.createPayment(
                new BigDecimal("250.00"),
                "INR",
                "payment-123");

        assertEquals("order_test_123", orderId);
        mockServer.verify();
    }

    @Test
    void createPayment_withNonInrCurrency_rejectsRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.createPayment(
                        new BigDecimal("10.00"),
                        "USD",
                        "payment-456"));
    }
}
