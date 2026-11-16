package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionLifecycleTest {
    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final SubscriptionLifecycle lifecycle = new SubscriptionLifecycle(subscriptions, audit,
            Clock.fixed(now, ZoneOffset.UTC), 7, 7, 30);
    private TenantSubscription subscription(SubscriptionStatus status) {
        Tenant tenant = new Tenant(); tenant.setId(1L);
        TenantSubscription s = new TenantSubscription(); s.setTenant(tenant); s.setStatus(status); return s;
    }
    @Test void validatesEveryPairInApprovedMatrix() {
        Set<String> allowed = Set.of("TRIAL:ACTIVE", "TRIAL:SUSPENDED", "ACTIVE:PAST_DUE", "ACTIVE:CANCELLED",
                "PAST_DUE:ACTIVE", "PAST_DUE:SUSPENDED", "SUSPENDED:ACTIVE", "SUSPENDED:CANCELLED");
        for (SubscriptionStatus from : SubscriptionStatus.values()) {
            for (SubscriptionStatus to : SubscriptionStatus.values()) {
                if (from == to || allowed.contains(from + ":" + to)) {
                    assertDoesNotThrow(() -> SubscriptionLifecycle.validate(from, to, false, now.plusSeconds(1), now));
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> SubscriptionLifecycle.validate(from, to, false, now.plusSeconds(1), now), from + ":" + to);
                }
            }
        }
    }
    @Test void cancelledRequiresFreshOnboardingWithinRetention() {
        assertDoesNotThrow(() -> SubscriptionLifecycle.validate(SubscriptionStatus.CANCELLED, SubscriptionStatus.ACTIVE, true, now.plusSeconds(1), now));
        assertThrows(IllegalArgumentException.class, () -> SubscriptionLifecycle.validate(SubscriptionStatus.CANCELLED, SubscriptionStatus.ACTIVE, true, now, now));
        assertThrows(IllegalArgumentException.class, () -> SubscriptionLifecycle.validate(SubscriptionStatus.CANCELLED, SubscriptionStatus.TRIAL, true, null, now));
    }
    @Test void duplicatePastDueDoesNotExtendGrace() {
        TenantSubscription s = subscription(SubscriptionStatus.ACTIVE);
        lifecycle.transition(s, SubscriptionStatus.PAST_DUE, now, false, "test");
        lifecycle.transition(s, SubscriptionStatus.PAST_DUE, now.plusSeconds(300), false, "duplicate");
        assertEquals(now.plus(Duration.ofDays(7)), s.getGraceEndsAt());
        assertTrue(s.getTenant().isActive());
        verify(audit, times(1)).save(any());
    }
    @Test void downtimeDoesNotRestartGraceOrRetention() {
        TenantSubscription s = subscription(SubscriptionStatus.TRIAL);
        s.setTrialEndsAt(LocalDate.of(2026, 9, 1));
        lifecycle.reconcile(s);
        assertEquals(SubscriptionStatus.CANCELLED, s.getStatus());
        assertEquals(Instant.parse("2026-10-08T00:00:00Z"), s.getRetentionEndsAt());
        assertFalse(s.getTenant().isActive());
        assertEquals("CANCELLED", s.getTenant().getStatus());
        verify(audit, times(2)).save(any());
    }
    @Test void missingHistoricalDeadlinesAreNotInvented() {
        TenantSubscription s = subscription(SubscriptionStatus.PAST_DUE);
        lifecycle.reconcile(s);
        assertEquals(SubscriptionStatus.PAST_DUE, s.getStatus());
        verifyNoInteractions(audit, subscriptions);
    }
}
