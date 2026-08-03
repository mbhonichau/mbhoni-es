package com.mbhoni_creative.config;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.ModuleService;

@ExtendWith(MockitoExtension.class)
class TenantEntitlementServiceTest {

    @Mock
    private TenantSecurityService tenantSecurityService;

    @Mock
    private ModuleService moduleService;

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @InjectMocks
    private TenantEntitlementService tenantEntitlementService;

    @Test
    void testIsModuleEnabled_GlobalAdmin_ReturnsTrue() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);

        boolean enabled = tenantEntitlementService.isModuleEnabled("EMPLOYEE");

        assertTrue(enabled);
    }

    @Test
    void testIsModuleEnabled_TenantUser_ChecksModuleService() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(false);
        when(tenantSecurityService.getCurrentTenantId()).thenReturn(1L);
        when(moduleService.isModuleEnabled(1L, "EMPLOYEE")).thenReturn(true);

        boolean enabled = tenantEntitlementService.isModuleEnabled("EMPLOYEE");

        assertTrue(enabled);
    }

    @Test
    void testIsFeatureEnabled_Branding_Allowed() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(false);
        when(tenantSecurityService.getCurrentTenantId()).thenReturn(1L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setBrandingEnabled(true);

        TenantSubscription subscription = new TenantSubscription();
        subscription.setPlan(plan);

        when(tenantSubscriptionRepository.findByTenantId(1L)).thenReturn(Optional.of(subscription));

        assertTrue(tenantEntitlementService.isFeatureEnabled("BRANDING"));
    }

    @Test
    void testIsFeatureEnabled_ApiAccess_Disabled() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(false);
        when(tenantSecurityService.getCurrentTenantId()).thenReturn(1L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setApiAccessEnabled(false);

        TenantSubscription subscription = new TenantSubscription();
        subscription.setPlan(plan);

        when(tenantSubscriptionRepository.findByTenantId(1L)).thenReturn(Optional.of(subscription));

        assertFalse(tenantEntitlementService.isFeatureEnabled("API_ACCESS"));
    }
}
