package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationIsolationTest {
    private final OrganizationUnitRepository repository = mock(OrganizationUnitRepository.class);
    private final OrganizationUnitServiceImpl service = new OrganizationUnitServiceImpl(repository,
            mock(TenantRepository.class), new TenantAccessService(new TenantSecurityService()),
            mock(OrganizationAccessService.class), mock(TenantGovernance.class), new TenantSecurityService());

    @BeforeEach void authenticate() {
        Tenant tenant = new Tenant(); tenant.setId(1L);
        User user = new User(); user.setTenant(tenant);
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void foreignTenantListIsRejectedBeforeQuery() {
        assertThrows(AccessDeniedException.class, () -> service.getOrganizationUnitsByTenant(2L));
        verifyNoInteractions(repository);
    }
    @Test void omittedTenantUsesAuthenticatedTenant() {
        when(repository.findByTenantId(1L)).thenReturn(List.of());
        assertEquals(List.of(), service.getOrganizationUnitsByTenant(null));
        verify(repository).findByTenantId(1L);
        verify(repository, never()).findAll();
    }
    @Test void missingContextDoesNotReturnEveryTenant() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.getOrganizationUnitsByTenant(null));
        verifyNoInteractions(repository);
    }
    @Test void foreignResourceIsRejected() {
        Tenant tenant = new Tenant(); tenant.setId(2L);
        OrganizationUnit unit = new OrganizationUnit(); unit.setTenant(tenant);
        when(repository.findById(7L)).thenReturn(Optional.of(unit));
        assertThrows(AccessDeniedException.class, () -> service.getOrganizationUnitById(7L));
    }
}
