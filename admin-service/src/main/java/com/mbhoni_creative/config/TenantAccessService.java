package com.mbhoni_creative.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.security.TenantContext;

/**
 * Centralizes authorization for tenant-owned resources.
 *
 * Tenant users and API keys may access only their assigned tenant. Global
 * administrators retain cross-tenant access for platform administration.
 */
@Service
public class TenantAccessService {

    private final TenantSecurityService tenantSecurityService;

    public TenantAccessService(TenantSecurityService tenantSecurityService) {
        this.tenantSecurityService = tenantSecurityService;
    }

    public void requireAccess(Long targetTenantId) {
        if (targetTenantId == null) {
            throw new AccessDeniedException("A tenant is required for this resource");
        }

        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        Long authenticatedTenantId = null;

        if (principal instanceof CustomUserPrincipal userPrincipal) {
            authenticatedTenantId = userPrincipal.getTenantId();
        } else if (principal instanceof ApiKeyPrincipal apiKeyPrincipal) {
            authenticatedTenantId = apiKeyPrincipal.getTenantId();
        }

        if (!targetTenantId.equals(authenticatedTenantId)) {
            throw new AccessDeniedException("You do not have access to this tenant");
        }
    }

    public Long resolveAccessibleTenantId(Long requestedTenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return requestedTenantId;
        }

        Long currentTenantId = getCurrentTenantId();
        Long effectiveTenantId = requestedTenantId != null ? requestedTenantId : currentTenantId;
        requireAccess(effectiveTenantId);
        return effectiveTenantId;
    }

    public void requireAccessIfTenantOwned(Long tenantId) {
        if (tenantId != null) {
            requireAccess(tenantId);
        }
    }

    public Long getCurrentTenantId() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return TenantContext.getTenantId();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.getTenantId();
        }
        if (principal instanceof ApiKeyPrincipal apiKeyPrincipal) {
            return apiKeyPrincipal.getTenantId();
        }
        return null;
    }
}
