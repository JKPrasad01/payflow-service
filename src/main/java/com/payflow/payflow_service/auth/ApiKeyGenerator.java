package com.payflow.payflow_service.auth;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component 
public class ApiKeyGenerator {
  
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    public record GeneratedKey(String rawKey,String prefix, String hash) {}

    public GeneratedKey generate(){

        String prefix = B64.encodeToString(randomBytes(6));
        String secret = B64.encodeToString(randomBytes(32));
        String rawKey = "pfk_" + prefix + "." + secret;
        return new GeneratedKey(rawKey, prefix, ApiKeyHasher.sha256Hex(rawKey));
    }

    private static byte[] randomBytes(int length){
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
