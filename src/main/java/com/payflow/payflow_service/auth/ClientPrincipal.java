package com.payflow.payflow_service.auth;

import java.util.UUID;

/**
 * The authenticated caller. Later days read clientAppId from here for tenant
 * isolation.
 */
public record ClientPrincipal(UUID clientAppId, String name) {
}