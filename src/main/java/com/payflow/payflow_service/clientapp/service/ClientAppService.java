package com.payflow.payflow_service.clientapp.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.payflow.payflow_service.auth.ApiKeyAuthService;
import com.payflow.payflow_service.auth.ApiKeyGenerator;
import com.payflow.payflow_service.clientapp.entity.ClientApp;
import com.payflow.payflow_service.clientapp.repository.ClientAppRepository;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import jakarta.transaction.Transactional;

@Service
public class ClientAppService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClientAppRepository repository;
    private final ApiKeyGenerator keyGenerator;
    private final ApiKeyAuthService authService;

    public ClientAppService(ClientAppRepository repository,
            ApiKeyGenerator keyGenerator,
            ApiKeyAuthService authService) {
        this.repository = repository;
        this.keyGenerator = keyGenerator;
        this.authService = authService;
    }

    /**
     * The raw apiKey and webhookSecret exist only in this return value. The key is
     * never stored in plain text.
     */
    public record RegisteredClient(UUID id, String name, String keyPrefix, String apiKey, String webhookSecret) {
    }

    @Transactional
    public RegisteredClient register(String name, String callbackUrl) {
        ApiKeyGenerator.GeneratedKey key = keyGenerator.generate();
        String webhookSecret = "whsec_" + randomToken(32);
        ClientApp app = repository.save(new ClientApp(name, key.prefix(), key.hash(), callbackUrl, webhookSecret));
        return new RegisteredClient(app.getId(), app.getName(), key.prefix(), key.rawKey(), webhookSecret);
    }

    @Transactional
    public void revoke(UUID id) {
        ClientApp app = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client app not found"));
        app.revoke();
        // Evict the auth cache only AFTER the commit. Evicting earlier lets a
        // concurrent request
        // reload the still-ACTIVE row and cache it for a full TTL.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authService.invalidateAll();
            }
        });
    }

    private static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
