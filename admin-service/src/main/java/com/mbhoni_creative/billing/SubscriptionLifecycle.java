package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.time.*;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionLifecycle {
    private final TenantSubscriptionRepository subscriptions;
    private final AuditLogRepository audit;
    private final Clock clock;
    private final int paymentGraceDays, suspendedGraceDays, retentionDays;
    public SubscriptionLifecycle(TenantSubscriptionRepository subscriptions, AuditLogRepository audit, Clock clock,
            @Value("${app.lifecycle.payment-grace-days:7}") int paymentGraceDays,
            @Value("${app.lifecycle.suspended-grace-days:7}") int suspendedGraceDays,
            @Value("${app.lifecycle.retention-days:30}") int retentionDays) {
        if (paymentGraceDays < 1 || suspendedGraceDays < 1 || retentionDays < 1) throw new IllegalArgumentException("Lifecycle durations must be positive");
        this.subscriptions = subscriptions; this.audit = audit; this.clock = clock;
        this.paymentGraceDays = paymentGraceDays; this.suspendedGraceDays = suspendedGraceDays; this.retentionDays = retentionDays;
    }
    public static void validate(SubscriptionStatus from, SubscriptionStatus to, boolean freshOnboarding,
                                Instant retentionEndsAt, Instant now) {
        if (from == null || to == null) throw new IllegalArgumentException("Subscription state is required");
        if (from == to) return;
        boolean allowed = switch (from) {
            case TRIAL -> Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED).contains(to);
            case ACTIVE -> Set.of(SubscriptionStatus.PAST_DUE, SubscriptionStatus.CANCELLED).contains(to);
            case PAST_DUE -> Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED).contains(to);
            case SUSPENDED -> Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED).contains(to);
            case CANCELLED -> freshOnboarding && retentionEndsAt != null && now.isBefore(retentionEndsAt)
                    && Set.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE).contains(to);
        };
        if (!allowed) throw new IllegalArgumentException("Invalid subscription transition: " + from + " -> " + to);
    }
    public void transition(TenantSubscription subscription, SubscriptionStatus target, Instant at,
                           boolean freshOnboarding, String source) {
        SubscriptionStatus previous = subscription.getStatus();
        validate(previous, target, freshOnboarding, subscription.getRetentionEndsAt(), clock.instant());
        if (previous == target) return; // Duplicate events must not extend grace periods.
        subscription.setStatus(target);
        subscription.setStateChangedAt(at);
        subscription.setGraceEndsAt(switch (target) {
            case PAST_DUE -> at.plus(Duration.ofDays(paymentGraceDays));
            case SUSPENDED -> at.plus(Duration.ofDays(suspendedGraceDays));
            default -> null;
        });
        subscription.setRetentionEndsAt(target == SubscriptionStatus.CANCELLED ? at.plus(Duration.ofDays(retentionDays)) : null);
        subscription.getTenant().setStatus(target.name());
        subscription.getTenant().setActive(target == SubscriptionStatus.TRIAL || target == SubscriptionStatus.ACTIVE || target == SubscriptionStatus.PAST_DUE);
        subscriptions.save(subscription);
        AuditLog record = new AuditLog();
        record.setTenantId(subscription.getTenant().getId()); record.setUsername("billing-service");
        record.setActorType("BILLING_SERVICE"); record.setAction("SUBSCRIPTION_TRANSITION");
        record.setResourceType("TENANT"); record.setResourceId(String.valueOf(subscription.getTenant().getId()));
        record.setDescription(previous + " -> " + target + "; " + source);
        record.setSuccess(true); record.setOccurredAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        audit.save(record);
    }
    @Transactional
    public void reconcile(Long tenantId) {
        TenantSubscription subscription = subscriptions.lockByTenantId(tenantId).orElseThrow();
        reconcile(subscription);
    }
    public void reconcile(TenantSubscription subscription) {
        Instant now = clock.instant();
        if (subscription.getStatus() == SubscriptionStatus.TRIAL && subscription.getTrialEndsAt() != null) {
            Instant expiry = subscription.getTrialEndsAt().atStartOfDay(ZoneOffset.UTC).toInstant();
            if (!now.isBefore(expiry)) transition(subscription, SubscriptionStatus.SUSPENDED, expiry, false, "trial-expired");
        }
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE && subscription.getGraceEndsAt() != null
                && !now.isBefore(subscription.getGraceEndsAt())) {
            transition(subscription, SubscriptionStatus.SUSPENDED, subscription.getGraceEndsAt(), false, "payment-grace-expired");
        }
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED && subscription.getGraceEndsAt() != null
                && !now.isBefore(subscription.getGraceEndsAt())) {
            transition(subscription, SubscriptionStatus.CANCELLED, subscription.getGraceEndsAt(), false, "suspension-grace-expired");
        }
    }

    public record AccessState(SubscriptionStatus status, Instant graceEndsAt, Instant retentionEndsAt) {
        public boolean fullAccess() { return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAST_DUE; }
    }
    /** Evaluate deadlines on every request, even if the scheduled writer has not run yet. */
    public AccessState accessState(TenantSubscription subscription) {
        if (subscription == null || subscription.getStatus() == null) return new AccessState(SubscriptionStatus.SUSPENDED, null, null);
        SubscriptionStatus status = subscription.getStatus();
        Instant grace = subscription.getGraceEndsAt(), retention = subscription.getRetentionEndsAt(), now = clock.instant();
        if (status == SubscriptionStatus.TRIAL) {
            if (subscription.getTrialEndsAt() == null) return new AccessState(SubscriptionStatus.SUSPENDED, null, null);
            Instant expiry = subscription.getTrialEndsAt().atStartOfDay(ZoneOffset.UTC).toInstant();
            if (!now.isBefore(expiry)) { status = SubscriptionStatus.SUSPENDED; grace = expiry.plus(Duration.ofDays(suspendedGraceDays)); }
        }
        if (status == SubscriptionStatus.PAST_DUE) {
            if (grace == null) return new AccessState(SubscriptionStatus.SUSPENDED, null, null);
            if (!now.isBefore(grace)) { status = SubscriptionStatus.SUSPENDED; grace = grace.plus(Duration.ofDays(suspendedGraceDays)); }
        }
        if (status == SubscriptionStatus.SUSPENDED && grace != null && !now.isBefore(grace)) {
            status = SubscriptionStatus.CANCELLED; retention = grace.plus(Duration.ofDays(retentionDays)); grace = null;
        }
        if ((status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.PAST_DUE)
                && !subscription.getTenant().isActive()) return new AccessState(SubscriptionStatus.SUSPENDED, null, null);
        return new AccessState(status, grace, retention);
    }
}
