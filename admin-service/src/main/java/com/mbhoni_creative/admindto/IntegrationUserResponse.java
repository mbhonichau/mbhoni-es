package com.mbhoni_creative.admindto;

import java.util.Set;

public class IntegrationUserResponse {

    private Long id;
    private Long tenantId;
    private String username;
    private String email;
    private boolean active;
    private Set<String> roles;

    public IntegrationUserResponse(
            Long id,
            Long tenantId,
            String username,
            String email,
            boolean active,
            Set<String> roles) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.active = active;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
