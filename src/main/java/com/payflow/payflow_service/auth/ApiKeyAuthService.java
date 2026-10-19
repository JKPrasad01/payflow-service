package com.payflow.payflow_service.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.payflow.payflow_service.clientapp.enums.ClientAppStatus;
import com.payflow.payflow_service.clientapp.repository.ClientAppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service 
public class ApiKeyAuthService {
  private static final Pattern KEY_FORMATE = Pattern.compile("^pfk_([A-Za-z0-9_-]{8})\\\\.[A-Za-z0-9_-]{43}$");
  
  private final ClientAppRepository repository;
  // Keyed by SHA-256 of the presented key, so the raw key is never held in
                                            // memory as a cache key.
  // Bounded size: random garbage keys cannot grow it without limit. Negative
  // results are cached too.
  private final Cache<String, Optional<ClientPrincipal>> cache;

    @Autowired
    public ApiKeyAuthService(ClientAppRepository repository,
                             @Value ("${payflow.auth.cache-ttl-seconds:30}") long ttlSeconds) {
        this(repository, Duration.ofSeconds(ttlSeconds));
    }

    ApiKeyAuthService(ClientAppRepository repository, Duration ttl) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build();
    }

    public Optional<ClientPrincipal> authenticate(String rawKey){

        if(rawKey == null){
            return Optional.empty();
        }

        Matcher matcher = KEY_FORMATE.matcher(rawKey);
        if(!matcher.matches()){
            return Optional.empty(); // malformed keys never reach the database
        }
        String prefix = matcher.group(1);
        String presentedHash = ApiKeyHasher.sha256Hex(rawKey);
        return cache.get(presentedHash, hash -> lookup(prefix, hash));
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    private Optional<ClientPrincipal> lookup(String prefix, String presentedHash) {
        return repository.findByKeyPrefix(prefix)
                .filter(app -> app.getStatus() == ClientAppStatus.ACTIVE)
                .filter(app -> ApiKeyHasher.constantTimeEquals(app.getKeyHash(), presentedHash))
                .map(app -> new ClientPrincipal(app.getId(), app.getName()));
    }

}
