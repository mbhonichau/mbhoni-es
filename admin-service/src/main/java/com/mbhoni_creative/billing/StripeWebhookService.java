package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.*;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import java.time.*;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class StripeWebhookService {
    private final StripeSignatureVerifier verifier;
    private final ObjectMapper mapper;
    private final StripeGateway stripe;
    private final TenantSubscriptionRepository subscriptions;
    private final WebhookReceiptRepository receipts;
    private final SubscriptionLifecycle lifecycle;
    private final Clock clock;
    private final StripePlanResolver plans;
    private final com.mbhoni_creative.adminservice.AuditLogService audit;
    private final StripeInvoiceService invoices;
    public StripeWebhookService(StripeSignatureVerifier verifier, ObjectMapper mapper, StripeGateway stripe,
            TenantSubscriptionRepository subscriptions, WebhookReceiptRepository receipts, SubscriptionLifecycle lifecycle, Clock clock,
            StripePlanResolver plans, com.mbhoni_creative.adminservice.AuditLogService audit, StripeInvoiceService invoices) {
        this.verifier = verifier; this.mapper = mapper; this.stripe = stripe; this.subscriptions = subscriptions;
        this.receipts = receipts; this.lifecycle = lifecycle; this.clock = clock;
        this.plans = plans; this.audit = audit;
        this.invoices = invoices;
    }
    @Transactional
    public void receive(byte[] body, String signature) {
        verifier.verify(body, signature);
        JsonNode event;
        try { event = mapper.readTree(body); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe JSON"); }
        String id = event.path("id").asText("");
        if (!id.matches("evt_[A-Za-z0-9]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID required");
        if (receipts.existsById(id)) return;
        String type = event.path("type").asText();
        if (!Set.of("invoice.paid", "invoice.payment_succeeded", "invoice.payment_failed", "customer.subscription.updated", "customer.subscription.deleted").contains(type)) return;
        JsonNode object = event.path("data").path("object");
        String stripeId = type.startsWith("customer.subscription.") ? StripeGateway.id(object)
                : StripeGateway.id(object.path("parent").path("subscription_details").path("subscription"));
        if (stripeId.isBlank()) stripeId = StripeGateway.id(object.path("subscription")); // pre-Basil invoices
        TenantSubscription found = subscriptions.findByStripeSubscriptionId(stripeId).orElse(null);
        // This account can contain unrelated products. Linking explicitly reconciles the current state.
        if (found == null) return;
        TenantSubscription subscription = subscriptions.lockByTenantId(found.getTenant().getId()).orElseThrow();
        if (!stripeId.equals(subscription.getStripeSubscriptionId())) return; // concurrent relink
        if (receipts.existsById(id)) return; // serialized with the tenant row lock
        JsonNode current = stripe.subscription(stripeId);
        sync(subscription, current, false, "stripe:" + id);
        receipts.save(new WebhookReceipt(id, clock.instant(), "RECONCILED"));
    }
    public void sync(TenantSubscription local, JsonNode current, boolean freshOnboarding, String source) {
        if (!local.getStripeSubscriptionId().equals(StripeGateway.id(current)) ||
                !local.getStripeCustomerId().equals(StripeGateway.id(current.path("customer")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stripe subscription/customer mapping mismatch");
        }
        SubscriptionStatus state = local.getStatus();
        String remote = current.path("status").asText();
        invoices.sync(local);
        if (state == SubscriptionStatus.CANCELLED && !freshOnboarding) return;
        if ("active".equals(remote)
                && "paid".equals(current.path("latest_invoice").path("status").asText())) {
            SubscriptionPlan plan = plans.resolve(current);
            StripeSubscriptionSnapshot.apply(local, current, clock.instant());
            lifecycle.transition(local, SubscriptionStatus.ACTIVE, clock.instant(), freshOnboarding, source);
            if (local.getPlan() == null || !java.util.Objects.equals(local.getPlan().getId(), plan.getId())) {
                local.setPlan(plan);
                audit.logForTenant(local.getTenant().getId(), "STRIPE_PLAN_CHANGED", "TENANT", String.valueOf(local.getTenant().getId()),
                        "Verified paid Stripe plan " + plan.getCode() + "; " + source);
            }
        } else if ("past_due".equals(remote) && state == SubscriptionStatus.ACTIVE) {
            lifecycle.transition(local, SubscriptionStatus.PAST_DUE, clock.instant(), false, source);
        } else if ("canceled".equals(remote) && state == SubscriptionStatus.ACTIVE) {
            lifecycle.transition(local, SubscriptionStatus.CANCELLED, clock.instant(), false, source);
        } else if (Set.of("paused", "canceled", "incomplete_expired", "unpaid").contains(remote) && state == SubscriptionStatus.TRIAL) {
            lifecycle.transition(local, SubscriptionStatus.SUSPENDED, clock.instant(), false, source);
        } else if ("unpaid".equals(remote) && state == SubscriptionStatus.ACTIVE) {
            lifecycle.transition(local, SubscriptionStatus.PAST_DUE, clock.instant(), false, source);
        }
        // Never grant an unpaid upgrade, but keep operational provider state visible.
        local.setProviderStatus(remote);
        local.setProviderSyncedAt(clock.instant());
        local.setCancelAtPeriodEnd(current.path("cancel_at_period_end").asBoolean());
        subscriptions.save(local);
        // Cancellation during dunning preserves both the payment and suspended grace periods.
        lifecycle.reconcile(local);
    }
}
