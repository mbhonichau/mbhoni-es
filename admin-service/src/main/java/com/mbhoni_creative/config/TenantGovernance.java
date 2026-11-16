package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.TenantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantGovernance {
    private final TenantRepository tenants;
    private final TenantSecurityService security;
    public TenantGovernance(TenantRepository tenants, TenantSecurityService security) {
        this.tenants = tenants; this.security = security;
    }
    public User caller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserPrincipal principal))
            throw new AccessDeniedException("An authenticated user is required");
        return principal.getUser();
    }
    public boolean isOwner(Long tenantId) {
        User user = caller();
        return user.getId() != null && user.getTenant() != null && tenantId.equals(user.getTenant().getId())
                && tenants.findById(tenantId).map(t -> user.getId().equals(t.getOwnerUserId())).orElse(false);
    }
    public boolean isTopAdmin(Long tenantId) {
        User user = caller();
        return user.getTenant() != null && tenantId.equals(user.getTenant().getId()) && user.getRoles().stream().anyMatch(r ->
                r.isSystemRole() && "ROLE_TENANT_ADMIN".equals(r.getName())
                && (r.getTenant() == null || tenantId.equals(r.getTenant().getId())));
    }
    public void requireOwner(Long tenantId) {
        if (tenantId == null || !isOwner(tenantId)) throw new AccessDeniedException("Tenant Owner required");
    }
    public void requireBillingOwner(Long tenantId) {
        if (!security.isGlobalAdmin()) requireOwner(tenantId);
    }
    public void requireDefaultsWrite(Long tenantId) {
        if (security.isGlobalAdmin()) return;
        if (tenantId == null || (!isOwner(tenantId) && !isTopAdmin(tenantId)))
            throw new AccessDeniedException("Platform defaults require platform admin; tenant overrides require Owner or top-level Admin");
    }
}
