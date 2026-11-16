package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.adminservice.impl.*;
import com.mbhoni_creative.config.TenantSecurityService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingOverrideAuthorizationTest {
    private final TenantSecurityService security = mock(TenantSecurityService.class);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final SubscriptionServiceImpl service = new SubscriptionServiceImpl(plans, subscriptions,
            mock(TenantRepository.class), mock(NotificationService.class), security,
            mock(SubscriptionLifecycle.class), mock(AuditLogService.class), Clock.systemUTC());
    @Test void tenantCannotModifySharedPlansOrAssignSubscriptions() {
        assertThrows(AccessDeniedException.class, () -> service.savePlan(new SubscriptionPlan()));
        assertThrows(AccessDeniedException.class, () -> service.updatePlan(1L, new SubscriptionPlan()));
        assertThrows(AccessDeniedException.class, () -> service.deactivatePlan(1L));
        assertThrows(AccessDeniedException.class, () -> service.deletePlan(1L));
        assertThrows(AccessDeniedException.class, () -> service.assignTenantSubscription(1L, 1L,
                SubscriptionStatus.ACTIVE, BillingCycle.MONTHLY, null, null, null, true, "unauthorized"));
        verifyNoInteractions(plans, subscriptions);
    }
    @Test void globalOverrideRequiresReasonBeforeLookingUpTenant() {
        when(security.isGlobalAdmin()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.assignTenantSubscription(1L, 1L,
                SubscriptionStatus.ACTIVE, BillingCycle.MONTHLY, null, null, null, true, " "));
        verifyNoInteractions(subscriptions);
    }
    @Test void usedPlanCannotBeDeletedAlongWithItsSubscriptions() {
        when(security.isGlobalAdmin()).thenReturn(true);
        when(subscriptions.existsByPlanId(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.deletePlan(1L));
        verify(plans, never()).delete(any()); verify(subscriptions, never()).deleteAll();
    }
    @Test void tenantCannotRunPlatformWideOverdueActions() {
        BillingServiceImpl billing = new BillingServiceImpl(mock(BillingAccountRepository.class), invoices,
                mock(PaymentRepository.class), mock(TenantRepository.class), subscriptions,
                mock(NotificationService.class), security, mock(SubscriptionLifecycle.class), mock(com.mbhoni_creative.config.TenantGovernance.class));
        assertThrows(AccessDeniedException.class, billing::refreshOverdueInvoices);
        assertThrows(AccessDeniedException.class, billing::suspendOverdueSubscriptions);
        verifyNoInteractions(invoices, subscriptions);
    }
}
