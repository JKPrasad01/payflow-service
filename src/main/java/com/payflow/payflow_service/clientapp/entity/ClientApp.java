package com.payflow.payflow_service.clientapp.entity;


import java.time.Instant;
import java.util.UUID;

import com.payflow.payflow_service.clientapp.enums.ClientAppStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_apps")
public class ClientApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    // Needed later to sign callbacks, so it cannot be hashed. See README "Known
    // gaps".
    @Column(name = "webhook_secret", nullable = false, length = 100)
    private String webhookSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientAppStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ClientApp() {
        // for JPA
    }

    public ClientApp(String name, String keyPrefix, String keyHash, String callbackUrl, String webhookSecret) {
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.callbackUrl = callbackUrl;
        this.webhookSecret = webhookSecret;
        this.status = ClientAppStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void revoke() {
        if (status == ClientAppStatus.REVOKED) {
            return;
        }
        this.status = ClientAppStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public ClientAppStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
