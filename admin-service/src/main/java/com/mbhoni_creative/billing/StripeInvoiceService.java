package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.TenantGovernance;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeInvoiceService {
    private final StripeGateway stripe;
    private final InvoiceRepository invoices;
    private final TenantSubscriptionRepository subscriptions;
    private final TenantGovernance governance;
    public StripeInvoiceService(StripeGateway stripe, InvoiceRepository invoices,
            TenantSubscriptionRepository subscriptions, TenantGovernance governance) {
        this.stripe = stripe; this.invoices = invoices; this.subscriptions = subscriptions; this.governance = governance;
    }
    @Transactional
    public void refresh(Long tenantId) {
        governance.requireBillingOwner(tenantId);
        sync(subscriptions.lockByTenantId(tenantId).orElseThrow());
    }
    // Internal callers already hold the subscription lock and have verified provider identity.
    public void sync(TenantSubscription subscription) {
        if (subscription.getStripeSubscriptionId() == null || subscription.getStripeCustomerId() == null)
            throw new IllegalStateException("Link the verified Stripe subscription before synchronizing invoices");
        String after = null;
        do {
            JsonNode page = stripe.invoices(subscription.getStripeCustomerId(), subscription.getStripeSubscriptionId(), after);
            if (page == null || !page.path("data").isArray()) throw new IllegalStateException("Invalid Stripe invoice response");
            String previous = after;
            for (JsonNode remote : page.path("data")) {
                copy(subscription, remote); after = remote.path("id").asText();
            }
            if (!page.path("has_more").asBoolean()) break;
            if (java.util.Objects.equals(previous, after)) throw new IllegalStateException("Stripe invoice pagination did not advance");
        } while (true);
    }
    void copy(TenantSubscription subscription, JsonNode remote) {
        String providerSubscription = StripeGateway.id(remote.path("parent").path("subscription_details").path("subscription"));
        if (providerSubscription.isBlank()) providerSubscription = StripeGateway.id(remote.path("subscription"));
        if (!subscription.getStripeCustomerId().equals(StripeGateway.id(remote.path("customer")))
                || !subscription.getStripeSubscriptionId().equals(providerSubscription))
            throw new IllegalArgumentException("Invoice provider identity mismatch");
        if (!"usd".equals(remote.path("currency").asText())) throw new IllegalArgumentException("Platform billing supports USD only");
        String id = remote.path("id").asText();
        if (!id.matches("in_[A-Za-z0-9]+")) throw new IllegalArgumentException("Stripe invoice ID required");
        Invoice local = invoices.findByStripeInvoiceId(id).orElseGet(Invoice::new);
        if (local.getTenant() != null && !local.getTenant().getId().equals(subscription.getTenant().getId()))
            throw new IllegalStateException("Stripe invoice already belongs to another tenant");
        local.setTenant(subscription.getTenant()); local.setSubscription(subscription); local.setStripeInvoiceId(id);
        // Provider ID is stable and globally unique; the full provider number remains in the snapshot.
        local.setInvoiceNumber(id); local.setCurrency("USD");
        local.setSubtotal(amount(remote, "subtotal")); local.setTotalAmount(amount(remote, "total"));
        local.setAmountPaid(amount(remote, "amount_paid")); local.setProviderAmountRemaining(amount(remote, "amount_remaining"));
        // Tax lines are provider-calculated integer minor units, never local rates or rounded line calculations.
        BigDecimal tax = BigDecimal.ZERO;
        JsonNode taxes = remote.has("total_taxes") ? remote.path("total_taxes") : remote.path("total_tax_amounts");
        if (!taxes.isArray()) throw new IllegalArgumentException("Provider tax breakdown required");
        for (JsonNode part : taxes) tax = tax.add(amount(part, "amount"));
        local.setTaxAmount(tax);
        local.setStatus(switch (remote.path("status").asText()) {
            case "draft" -> InvoiceStatus.DRAFT;
            case "open" -> InvoiceStatus.ISSUED;
            case "paid" -> InvoiceStatus.PAID;
            case "void" -> InvoiceStatus.CANCELLED;
            case "uncollectible" -> InvoiceStatus.OVERDUE;
            default -> throw new IllegalArgumentException("Unknown Stripe invoice status");
        });
        local.setIssueDate(date(remote.path("created"))); local.setDueDate(date(remote.path("due_date")));
        String url = remote.path("hosted_invoice_url").asText(null);
        if (url != null) {
            var uri = java.net.URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"invoice.stripe.com".equals(uri.getHost()))
                throw new IllegalArgumentException("Unexpected provider invoice URL");
        }
        local.setHostedInvoiceUrl(url); local.setStripeInvoiceJson(remote.toString()); invoices.save(local);
    }
    private static LocalDate date(JsonNode value) {
        return value.isIntegralNumber() ? Instant.ofEpochSecond(value.longValue()).atZone(ZoneOffset.UTC).toLocalDate() : null;
    }
    static BigDecimal amount(JsonNode object, String name) {
        JsonNode value = object.path(name);
        if (!value.isIntegralNumber()) throw new IllegalArgumentException("Provider integer minor units required: " + name);
        return new BigDecimal(value.bigIntegerValue(), 2);
    }
}
