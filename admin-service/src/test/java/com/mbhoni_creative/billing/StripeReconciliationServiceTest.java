package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeReconciliationServiceTest {
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC);
    private final SubscriptionLifecycle lifecycle = new SubscriptionLifecycle(subscriptions, mock(AuditLogRepository.class), clock, 7, 7, 30);
    private StripeReconciliationService service() {
        var webhook = new StripeWebhookService(mock(StripeSignatureVerifier.class), new ObjectMapper(), stripe, subscriptions,
                mock(WebhookReceiptRepository.class), lifecycle, clock,
                new StripePlanResolver(mock(SubscriptionPlanRepository.class), new MockEnvironment()), audit, mock(StripeInvoiceService.class));
        return new StripeReconciliationService(subscriptions, stripe, webhook, audit);
    }
    private TenantSubscription local(SubscriptionStatus status) {
        var s = new TenantSubscription(); var tenant = new Tenant(); tenant.setId(1L); s.setTenant(tenant);
        s.setStatus(status); s.setStripeCustomerId("cus_one"); s.setStripeSubscriptionId("sub_one");
        when(subscriptions.lockByTenantId(1L)).thenReturn(Optional.of(s)); return s;
    }
    @Test void missedFailureWebhookIsRepairedFromProvider() throws Exception {
        var s = local(SubscriptionStatus.ACTIVE);
        when(stripe.subscription("sub_one")).thenReturn(new ObjectMapper().readTree("{\"id\":\"sub_one\",\"customer\":\"cus_one\",\"status\":\"past_due\"}"));
        service().reconcile(1L);
        assertEquals(SubscriptionStatus.PAST_DUE, s.getStatus());
        assertEquals(clock.instant().plus(Duration.ofDays(7)), s.getGraceEndsAt());
        verify(audit).logForTenant(eq(1L), eq("STRIPE_DRIFT_REPAIRED"), anyString(), anyString(), anyString());
    }
    @Test void providerOutageDoesNotAdvanceDeadlinesOrPurgeButOtherTenantsProceed() {
        var provider = mock(StripeReconciliationService.class); var localLifecycle = mock(SubscriptionLifecycle.class);
        var purge = mock(TenantPurgeService.class);
        when(subscriptions.findStripeLinkedTenantIds()).thenReturn(List.of(1L));
        when(subscriptions.findLifecycleCandidateTenantIds()).thenReturn(List.of(1L, 2L));
        when(subscriptions.findExpiredRetentionTenantIds(clock.instant())).thenReturn(List.of(1L));
        doThrow(new IllegalStateException("network")).when(provider).reconcile(1L);
        new LifecycleScheduler(subscriptions, localLifecycle, purge, clock, provider).reconcile();
        verify(localLifecycle, never()).reconcile(1L); verify(localLifecycle).reconcile(2L); verifyNoInteractions(purge);
    }
    @Test void cancelledProviderMismatchRequiresInvestigation() throws Exception {
        local(SubscriptionStatus.CANCELLED);
        when(stripe.subscription("sub_one")).thenReturn(new ObjectMapper().readTree("{\"id\":\"sub_one\",\"customer\":\"cus_one\",\"status\":\"active\"}"));
        assertThrows(IllegalStateException.class, () -> service().reconcile(1L));
        verify(subscriptions, never()).save(any());
    }
}

