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

class SharedCatalogAuthorizationTest {
    final TenantSecurityService security = new TenantSecurityService();
    final TenantRepository tenants = mock(TenantRepository.class);
    final ServiceCatalogRepository catalogs = mock(ServiceCatalogRepository.class);
    final ServiceSlaRepository slas = mock(ServiceSlaRepository.class);
    final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    final PlatformModuleRepository modules = mock(PlatformModuleRepository.class);
    final SubscriptionPlanModuleRepository planModules = mock(SubscriptionPlanModuleRepository.class);
    final TenantModuleRepository tenantModules = mock(TenantModuleRepository.class);
    final ServiceCatalogServiceImpl catalog = new ServiceCatalogServiceImpl(catalogs, slas, security);
    final PlanModuleServiceImpl planService = new PlanModuleServiceImpl(plans, modules, planModules, security);
    final ModuleServiceImpl moduleService = new ModuleServiceImpl(modules, tenantModules, tenants, mock(TenantSubscriptionRepository.class),
            planModules, security, new TenantGovernance(tenants, security), new TenantAccessService(security));
    User user;
    @BeforeEach void setup() {
        user = new User(); user.setId(10L); Tenant tenant = ContractIsolationTest.tenant(1L); tenant.setOwnerUserId(99L); user.setTenant(tenant);
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        var principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void tenantCannotWriteGlobalCatalogOrSla() {
        assertThrows(AccessDeniedException.class, () -> catalog.saveService(new ServiceCatalog()));
        assertThrows(AccessDeniedException.class, () -> catalog.addSla(1L, new ServiceSla()));
        verifyNoInteractions(catalogs, slas);
    }
    @Test void tenantCannotChangeGlobalPlanEntitlementsOrCreateModule() {
        assertThrows(AccessDeniedException.class, () -> planService.updatePlanModules(1L, Set.of(2L)));
        assertThrows(AccessDeniedException.class, () -> moduleService.createModule(new PlatformModule()));
        verifyNoInteractions(plans, modules, planModules);
    }
    @Test void ordinaryMemberCannotChangeTenantWideModuleDefaults() {
        assertThrows(AccessDeniedException.class, () -> moduleService.updateTenantModules(1L, Set.of(2L)));
        verifyNoInteractions(tenantModules);
    }
    @Test void platformAdminCanMaintainSharedCatalog() {
        user.setGlobalAdmin(true); ServiceCatalog item = new ServiceCatalog(); when(catalogs.save(item)).thenReturn(item);
        assertSame(item, catalog.saveService(item));
    }
}
