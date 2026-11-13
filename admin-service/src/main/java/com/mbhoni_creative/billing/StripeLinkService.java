package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.TenantSecurityService;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class StripeLinkService {
    private final TenantSecurityService security;
    private final TenantSubscriptionRepository subscriptions;
    private final SubscriptionPlanRepository plans;
    private final StripeGateway stripe;
    private final StripeWebhookService webhooks;
    private final AuditLogService audit;
    private final Environment environment;
    private final Clock clock;
    private final StripePlanResolver planResolver;
    public StripeLinkService(TenantSecurityService security, TenantSubscriptionRepository subscriptions,
            SubscriptionPlanRepository plans, StripeGateway stripe, StripeWebhookService webhooks,
            AuditLogService audit, Environment environment, Clock clock, StripePlanResolver planResolver) {
        this.security = security; this.subscriptions = subscriptions; this.plans = plans;
        this.stripe = stripe; this.webhooks = webhooks; this.audit = audit; this.environment = environment; this.clock = clock;
        this.planResolver = planResolver;
    }
    @Transactional
    public void link(Long tenantId, String subscriptionId, String customerId, String reason) {
        if (!security.isGlobalAdmin()) throw new AccessDeniedException("Platform administrator required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Link justification required");
        TenantSubscription local = subscriptions.lockByTenantId(tenantId).orElseThrow();
        JsonNode remote = stripe.subscription(subscriptionId);
        if (!subscriptionId.equals(StripeGateway.id(remote)) || !customerId.equals(StripeGateway.id(remote.path("customer")))) {
            throw new IllegalArgumentException("Stripe identity mismatch");
        }
        if (!"active".equals(remote.path("status").asText())
                || !"paid".equals(remote.path("latest_invoice").path("status").asText())) {
            throw new IllegalArgumentException("A paid active subscription is required for conversion");
        }
        SubscriptionPlan plan = planResolver.resolve(remote);
        boolean fresh = local.getStatus() == SubscriptionStatus.CANCELLED && !subscriptionId.equals(local.getStripeSubscriptionId());
        SubscriptionLifecycle.validate(local.getStatus(), SubscriptionStatus.ACTIVE, fresh, local.getRetentionEndsAt(), clock.instant());
        local.setStripeCustomerId(customerId); local.setStripeSubscriptionId(subscriptionId); local.setPlan(plan);
        subscriptions.saveAndFlush(local); // Unique constraint prevents linking one subscription to two tenants.
        webhooks.sync(local, remote, fresh, "verified Stripe link");
        audit.logForTenant(tenantId, "STRIPE_SUBSCRIPTION_LINK", "TENANT", String.valueOf(tenantId),
                subscriptionId + ": " + reason.trim());
    }
}
