package com.payflow.payflow_service.common.api;

import java.time.Instant;

public record ApiErrorResponse(
        boolean success,
        String error,
        String message,
        Instant timestamp) {
    public static ApiErrorResponse failure(String error, String message) {
        return new ApiErrorResponse(
                false,
                error,
                message,
                Instant.now());
    }
}