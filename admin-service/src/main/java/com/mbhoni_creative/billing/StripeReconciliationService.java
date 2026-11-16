package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class StripeReconciliationService {
    private final TenantSubscriptionRepository subscriptions;
    private final StripeGateway stripe;
    private final StripeWebhookService webhooks;
    private final AuditLogService audit;
    public StripeReconciliationService(TenantSubscriptionRepository subscriptions, StripeGateway stripe,
            StripeWebhookService webhooks, AuditLogService audit) {
        this.subscriptions = subscriptions; this.stripe = stripe; this.webhooks = webhooks; this.audit = audit;
    }
    @Transactional
    public void reconcile(Long tenantId) {
        var local = subscriptions.lockByTenantId(tenantId).orElseThrow();
        if (local.getStripeSubscriptionId() == null) return;
        var remote = stripe.subscription(local.getStripeSubscriptionId());
        if (!local.getStripeCustomerId().equals(StripeGateway.id(remote.path("customer")))
                || !local.getStripeSubscriptionId().equals(StripeGateway.id(remote)))
            throw new IllegalStateException("Provider identity drift requires operator investigation");
        if (local.getStatus() == SubscriptionStatus.CANCELLED && !"canceled".equals(remote.path("status").asText()))
            throw new IllegalStateException("Cancelled local subscription differs from Stripe; do not purge or resurrect it automatically");
        String before = local.getStatus() + ":" + local.getBillingCycle() + ":" + local.getStripeItemsJson()
                + ":" + local.getCancelAtPeriodEnd() + ":" + local.getCurrentPeriodEnd();
        webhooks.sync(local, remote, false, "periodic-provider-reconciliation");
        String after = local.getStatus() + ":" + local.getBillingCycle() + ":" + local.getStripeItemsJson()
                + ":" + local.getCancelAtPeriodEnd() + ":" + local.getCurrentPeriodEnd();
        if (!Objects.equals(before, after)) audit.logForTenant(tenantId, "STRIPE_DRIFT_REPAIRED", "TENANT", String.valueOf(tenantId),
                "Provider subscription state, price components, interval or period differed; synchronized from Stripe");
    }
}
