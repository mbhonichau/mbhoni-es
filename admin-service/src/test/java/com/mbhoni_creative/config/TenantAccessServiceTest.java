package com.mbhoni_creative.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantApiKey;
import com.mbhoni_creative.adminentity.User;

class TenantAccessServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiKeyCannotAccessAnotherTenant() {
        TenantSecurityService tenantSecurityService = new TenantSecurityService();
        TenantAccessService tenantAccessService = new TenantAccessService(tenantSecurityService);

        Tenant tenant = new Tenant();
        tenant.setId(10L);
        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenant(tenant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(apiKey), null));

        assertDoesNotThrow(() -> tenantAccessService.requireAccess(10L));
        assertThrows(AccessDeniedException.class, () -> tenantAccessService.requireAccess(11L));
    }

    @Test
    void globalAdminCanAccessAnyTenant() {
        TenantSecurityService tenantSecurityService = new TenantSecurityService();
        TenantAccessService tenantAccessService = new TenantAccessService(tenantSecurityService);

        User user = new User();
        user.setGlobalAdmin(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserPrincipal(user), null));

        assertDoesNotThrow(() -> tenantAccessService.requireAccess(99L));
    }
}
