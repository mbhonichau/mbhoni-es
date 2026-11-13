package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantMetadataIsolationTest {
    final TenantRepository tenants = mock(TenantRepository.class);
    final TenantCustomizationRepository customizations = mock(TenantCustomizationRepository.class);
    final TenantOnboardingFieldRepository fields = mock(TenantOnboardingFieldRepository.class);
    final TenantSecurityService security = new TenantSecurityService();
    final TenantAccessService access = new TenantAccessService(security);
    final TenantGovernance governance = new TenantGovernance(tenants, security);
    final TenantCustomizationServiceImpl branding = new TenantCustomizationServiceImpl(customizations, tenants,
            mock(TenantSubscriptionRepository.class), security, access, governance);
    final TenantOnboardingFieldServiceImpl schemas = new TenantOnboardingFieldServiceImpl(fields, mock(IndustryProfileFieldRepository.class), tenants, security, access, governance);
    final ModuleServiceImpl modules = new ModuleServiceImpl(mock(PlatformModuleRepository.class), mock(TenantModuleRepository.class), tenants,
            mock(TenantSubscriptionRepository.class), mock(SubscriptionPlanModuleRepository.class), security, governance, access);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void authenticate(boolean key) {
        Tenant tenant = ContractIsolationTest.tenant(1L); Object principal;
        if (key) { TenantApiKey apiKey = new TenantApiKey(); apiKey.setTenant(tenant); principal = new ApiKeyPrincipal(apiKey); }
        else { User user = new User(); user.setTenant(tenant); principal = new CustomUserPrincipal(user); }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
    void denied(Long id) {
        assertThrows(AccessDeniedException.class, () -> branding.getBrandingResponse(id));
        assertThrows(AccessDeniedException.class, () -> modules.getTenantModulesResponse(id));
        assertThrows(AccessDeniedException.class, () -> modules.isModuleEnabled(id, "EMPLOYEE"));
        assertThrows(AccessDeniedException.class, () -> schemas.getOnboardingSchemaForTenant(id));
        verifyNoInteractions(fields, tenants);
    }
    @Test void anonymousCannotReadTenantMetadata() { denied(1L); }
    @Test void tenantUserCannotReadOtherTenantMetadata() { authenticate(false); denied(2L); }
    @Test void apiKeyCannotReadOtherTenantMetadata() { authenticate(true); denied(2L); }
    @Test void domainLookupStillChecksResolvedTenantOwnership() {
        authenticate(false); TenantCustomization foreign = new TenantCustomization(); foreign.setTenant(ContractIsolationTest.tenant(2L));
        when(customizations.findByCustomDomainIgnoreCase("b.example.com")).thenReturn(Optional.of(foreign));
        assertThrows(AccessDeniedException.class, () -> branding.getBrandingResponseByDomain("b.example.com"));
    }
    @Test void sameTenantApiKeyCanReadBranding() {
        authenticate(true); Tenant tenant = ContractIsolationTest.tenant(1L); tenant.setName("Acme");
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        assertEquals(1L, branding.getBrandingResponse(1L).getTenantId());
    }
}
