package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.TenantApiKey;

public class ApiKeyPrincipal {

    private final TenantApiKey apiKey;

    public ApiKeyPrincipal(TenantApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public TenantApiKey getApiKey() {
        return apiKey;
    }

    public Long getTenantId() {
        return apiKey.getTenant() != null ? apiKey.getTenant().getId() : null;
    }

    public String getName() {
        return apiKey.getName();
    }
}
