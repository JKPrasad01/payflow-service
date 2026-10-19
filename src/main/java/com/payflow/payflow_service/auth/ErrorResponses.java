package com.payflow.payflow_service.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

/**
 * One fixed 401 body, so a caller cannot tell a missing key from a wrong or
 * revoked one.
 */
final class ErrorResponses {

    private static final String UNAUTHORIZED_BODY = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"code\":\"UNAUTHORIZED\"}";

    private ErrorResponses() {
    }

    static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
