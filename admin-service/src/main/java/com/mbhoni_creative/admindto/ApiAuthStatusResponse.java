package com.mbhoni_creative.admindto;

import java.util.Set;

public class ApiAuthStatusResponse {

    private Long tenantId;
    private String apiKeyName;
    private Set<String> permissions;

    public ApiAuthStatusResponse(Long tenantId, String apiKeyName, Set<String> permissions) {
        this.tenantId = tenantId;
        this.apiKeyName = apiKeyName;
        this.permissions = permissions;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
