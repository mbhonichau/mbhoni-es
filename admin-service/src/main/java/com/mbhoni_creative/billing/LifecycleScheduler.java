package com.mbhoni_creative.billing;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class LifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(LifecycleScheduler.class);
    private final TenantSubscriptionRepository subscriptions;
    private final SubscriptionLifecycle lifecycle;
    private final TenantPurgeService purge;
    private final java.time.Clock clock;
    private final StripeReconciliationService provider;
    public LifecycleScheduler(TenantSubscriptionRepository subscriptions, SubscriptionLifecycle lifecycle, TenantPurgeService purge, java.time.Clock clock,
            StripeReconciliationService provider) {
        this.subscriptions = subscriptions; this.lifecycle = lifecycle;
        this.purge = purge; this.clock = clock;
        this.provider = provider;
    }
    @Scheduled(fixedDelayString = "${app.lifecycle.reconcile-ms:60000}")
    public void reconcile() {
        java.util.Set<Long> failed = new java.util.HashSet<>();
        for (Long tenantId : subscriptions.findStripeLinkedTenantIds()) {
            try { provider.reconcile(tenantId); }
            catch (RuntimeException ex) {
                failed.add(tenantId);
                log.error("Stripe reconciliation failed for tenant {}; deadlines/purge deferred and will retry", tenantId, ex);
            }
        }
        for (Long tenantId : subscriptions.findLifecycleCandidateTenantIds()) {
            if (failed.contains(tenantId)) continue;
            try { lifecycle.reconcile(tenantId); }
            catch (RuntimeException ex) { log.error("Lifecycle reconciliation failed for tenant {}", tenantId, ex); }
        }
        for (Long tenantId : subscriptions.findExpiredRetentionTenantIds(clock.instant())) {
            if (failed.contains(tenantId)) continue;
            try { purge.purgeExpired(tenantId); }
            catch (RuntimeException ex) { log.error("Expired tenant purge failed and will be retried for tenant {}", tenantId, ex); }
        }
    }
}
