
package com.payflow.payflow_service.payment.controller;

import com.payflow.payflow_service.common.exception.GlobalExceptionHandler;
import com.payflow.payflow_service.payment.dto.CreatePaymentRequest;
import com.payflow.payflow_service.payment.dto.CreatePaymentResponse;
import com.payflow.payflow_service.payment.entity.Payment;
import com.payflow.payflow_service.payment.enums.PaymentProvider;
import com.payflow.payflow_service.payment.enums.PaymentStatus;
import com.payflow.payflow_service.payment.mapper.PaymentMapper;
import com.payflow.payflow_service.payment.service.PaymentService;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @Test
    void createPayment_withValidRequest_returnsCreated() throws Exception {
        UUID paymentId = UUID.randomUUID();
        String providerOrderId = "order_test_123";

        CreatePaymentResponse response = new CreatePaymentResponse(
                paymentId,
                new BigDecimal("10.00"),
                "INR",
                PaymentStatus.CREATED,
                PaymentProvider.RAZORPAY,
                providerOrderId,
                Instant.parse("2026-10-17T06:00:00Z"));

        when(paymentService.createPayment(
                eq(new BigDecimal("10.00")),
                eq("INR"),
                eq("controller-test-1")))
                .thenReturn(mock(Payment.class));

        when(paymentMapper.toCreateResponse(any(Payment.class)))
                .thenReturn(response);

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("10.00"),
                "INR");

        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "controller-test-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.amount").value(10.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.provider").value("RAZORPAY"))
                .andExpect(jsonPath("$.data.providerOrderId")
                        .value(providerOrderId));
    }

    @Test
    void createPayment_withNegativeAmount_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "controller-test-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":-10.00,"currency":"INR"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createPayment_withInvalidCurrency_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "controller-test-3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":10.00,"currency":"inr"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createPayment_withoutIdempotencyKey_returnsMissingHeaderError()
            throws Exception {
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":10.00,"currency":"INR"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSING_REQUIRED_HEADER"));
    }

    @Test
    void createPayment_withMalformedJson_returnsInvalidRequestBody()
            throws Exception {
        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "controller-test-4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));
    }
}
