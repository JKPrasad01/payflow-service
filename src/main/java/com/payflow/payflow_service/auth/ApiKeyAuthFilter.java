package com.payflow.payflow_service.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthFilter extends OncePerRequestFilter{
  
    static final String HEADER  = "X-API-KEY";

    private final AuthKeyAuthService authService;

    ApiKeyAuthFilter(AuthKeyAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/admin/")          // admin token filter handles these
                || path.startsWith("/webhooks/")   // provider calls, verified by signature (Day 5)
                || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Optional<ClientPrincipal> principal = authService.authenticate(request.getHeader(HEADER));
        if (principal.isEmpty()) {
            ErrorResponses.unauthorized(response);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                principal.get(), null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
