package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.config.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantActivationAuthorizationTest {
    private final TenantRepository repository = mock(TenantRepository.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final com.mbhoni_creative.billing.TenantPurgeService purge = mock(com.mbhoni_creative.billing.TenantPurgeService.class);
    private final TenantServiceImpl service = new TenantServiceImpl(repository, new TenantSecurityService(),
            mock(IndustryProfileRepository.class), mock(NotificationService.class), mock(EntityManager.class), audit, purge);
    private Tenant tenant;
    private User caller;
    private TenantDto dto;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setName("Original"); tenant.setActive(false);
        caller = new User(); caller.setTenant(tenant);
        dto = new TenantDto(); dto.setName("Changed"); dto.setActive(true);
        when(repository.findById(1L)).thenReturn(Optional.of(tenant));
    }
    private void authenticate(boolean editor) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(caller), null,
                editor ? List.of(new SimpleGrantedAuthority("TENANT_EDIT")) : List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void ordinaryMemberCannotEditTenant() {
        authenticate(false);
        assertThrows(AccessDeniedException.class, () -> service.updateTenant(1L, dto));
        verify(repository, never()).save(any());
    }
    @Test void manualDeleteCannotBypassRetention() {
        caller.setGlobalAdmin(true); authenticate(false);
        assertThrows(IllegalStateException.class, () -> service.deleteTenant(1L));
        verify(purge).purgeExpired(1L); verify(repository, never()).delete(any());
    }
    @Test void tenantMemberCannotInvokeManualPurge() {
        authenticate(true);
        assertThrows(AccessDeniedException.class, () -> service.deleteTenant(1L)); verifyNoInteractions(purge);
    }
    @Test void tenantEditorCannotReactivateTenant() {
        authenticate(true);
        assertThrows(AccessDeniedException.class, () -> service.updateTenant(1L, dto));
        assertFalse(tenant.isActive()); assertEquals("Original", tenant.getName());
        verify(repository, never()).save(any());
    }
    @Test void globalOverrideRequiresJustification() {
        caller.setGlobalAdmin(true); authenticate(false);
        assertThrows(IllegalArgumentException.class, () -> service.updateTenant(1L, dto));
        verify(repository, never()).save(any());
    }
    @Test void justifiedGlobalOverrideIsAudited() {
        caller.setGlobalAdmin(true); authenticate(false); dto.setChangeReason("Support case 123");
        when(repository.save(tenant)).thenReturn(tenant);
        service.updateTenant(1L, dto);
        assertTrue(tenant.isActive());
        verify(audit).logForTenant(eq(1L), eq("TENANT_ACTIVATION_OVERRIDE"), eq("TENANT"), eq("1"),
                contains("Support case 123"));
    }
    @Test void authorizedTenantEditorCanChangeNameWithoutActivationOverride() {
        authenticate(true); dto.setActive(false);
        when(repository.save(tenant)).thenReturn(tenant);
        service.updateTenant(1L, dto);
        assertEquals("Changed", tenant.getName());
        verifyNoInteractions(audit);
    }
}
