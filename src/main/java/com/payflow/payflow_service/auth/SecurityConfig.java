package com.payflow.payflow_service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.ErrorResponse;

import jakarta.servlet.DispatcherType;

@Configuration 
@EnableWebSecurity 
public class SecurityConfig {


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            ApiKeyAuthService authService,
            @Value("${payflow.admin.token:}") String adminToken) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API-key auth, no cookies or sessions
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/webhooks/**").permitAll() // Day 5 adds signature verification
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, ex) -> ErrorResponses.unauthorized(response)))
                .addFilterBefore(new AdminTokenFilter(adminToken), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ApiKeyAuthFilter(authService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
  
}
