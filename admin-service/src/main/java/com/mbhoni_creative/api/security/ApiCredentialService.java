package com.mbhoni_creative.api.security;

import com.mbhoni_creative.adminservice.ApiKeyService;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ApiCredentialService {
    private final ApiKeyService keys;
    public ApiCredentialService(ApiKeyService keys) { this.keys = keys; }

    // The existing authenticate joins this read-only transaction: its lastUsedAt mutation is not flushed.
    @Transactional(readOnly = true)
    public ApiKeyPrincipal authenticate(String rawKey) {
        var key = keys.authenticate(rawKey);
        if (key == null || key.getTenant() == null || key.getTenant().getId() == null || !key.isActive()
                || key.getExpiresAt() != null && !LocalDateTime.now().isBefore(key.getExpiresAt()))
            throw new BadCredentialsException("Invalid API key");
        key.getTenant().isActive(); // initialize before leaving the read transaction
        key.getPermissions().size();
        return new ApiKeyPrincipal(key);
    }
}
