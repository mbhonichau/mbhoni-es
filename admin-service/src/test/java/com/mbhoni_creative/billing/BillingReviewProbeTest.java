package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.adminservice.impl.BillingServiceImpl;
import com.mbhoni_creative.config.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Review characterization probes: passing assertions document current gaps, not desired behavior. */
class BillingReviewProbeTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final SubscriptionLifecycle lifecycle = new SubscriptionLifecycle(subscriptions, mock(AuditLogRepository.class), clock, 7, 7, 30);
    private final MockEnvironment environment = new MockEnvironment().withProperty("app.stripe.price-plans.price_small", "SMALL");
    private final StripePlanResolver resolver = new StripePlanResolver(plans, environment);
    private final StripeWebhookService webhooks = new StripeWebhookService(mock(StripeSignatureVerifier.class), new ObjectMapper(),
            stripe, subscriptions, mock(WebhookReceiptRepository.class), lifecycle, clock, resolver, audit, mock(StripeInvoiceService.class));

    private SubscriptionPlan plan(long id, int seats, String price) {
        SubscriptionPlan p = new SubscriptionPlan(); ReflectionTestUtils.setField(p, "id", id);
        p.setCode("SMALL"); p.setMaxUsers(seats); p.setMonthlyPrice(new BigDecimal(price)); return p;
    }
    private TenantSubscription local() {
        Tenant tenant = new Tenant(); tenant.setId(1L); tenant.setActive(true);
        TenantSubscription s = new TenantSubscription(); s.setTenant(tenant); s.setPlan(plan(1, 20, "100.00"));
        s.setStripeSubscriptionId("sub_old"); s.setStripeCustomerId("cus_one");
        s.setStartDate(LocalDate.of(2026, 10, 1)); s.setEndDate(LocalDate.of(2026, 11, 1));
        return s;
    }
    private ObjectNode remote(String id) throws Exception {
        return (ObjectNode) new ObjectMapper().readTree("{\"id\":\"" + id + "\",\"customer\":\"cus_one\",\"status\":\"active\","
                + "\"items\":{\"data\":[{\"price\":\"price_small\",\"quantity\":10}]},"
                + "\"default_payment_method\":{\"type\":\"card\"},\"latest_invoice\":{\"status\":\"paid\"}}");
    }
    @Test void downgradePreservesExistingSeatsAndSynchronizesAnnualInterval() throws Exception {
        TenantSubscription s = local(); SubscriptionPlan small = plan(2, 5, "50.00");
        when(plans.findByCode("SMALL")).thenReturn(Optional.of(small));
        ObjectNode current = remote("sub_old");
        ((ObjectNode) current.path("items").path("data").get(0)).putObject("price").put("id", "price_small")
                .putObject("recurring").put("interval", "year");
        webhooks.sync(s, current, false, "review");
        assertSame(small, s.getPlan()); assertEquals(5, s.getPlan().getMaxUsers());
        assertEquals(BillingCycle.ANNUAL, s.getBillingCycle());
    }
    @Test void hybridWithTwoPriceItemsIsRejected() throws Exception {
        ObjectNode current = remote("sub_old");
        ((com.fasterxml.jackson.databind.node.ArrayNode) current.path("items").path("data")).addObject().put("price", "price_meter");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(current));
    }
    @Test void hybridWithMappedComponentsResolvesOnePlan() throws Exception {
        ObjectNode current = remote("sub_old");
        ((com.fasterxml.jackson.databind.node.ArrayNode) current.path("items").path("data")).addObject().put("price", "price_meter");
        SubscriptionPlan small = plan(2, 5, "50.00");
        when(plans.findByCode("SMALL")).thenReturn(Optional.of(small));
        environment.setProperty("app.stripe.price-plans.price_meter", "SMALL");
        assertSame(small, resolver.resolve(current));
    }
    @Test void cancelThenResubscribeRequiresNewSubscriptionAndUnexpiredRetention() throws Exception {
        TenantSubscription s = local(); lifecycle.transition(s, SubscriptionStatus.CANCELLED, clock.instant(), false, "review");
        when(subscriptions.lockByTenantId(1L)).thenReturn(Optional.of(s));
        when(plans.findByCode("SMALL")).thenReturn(Optional.of(plan(2, 5, "50.00")));
        when(stripe.subscription("sub_old")).thenReturn(remote("sub_old"));
        when(stripe.subscription("sub_new")).thenReturn(remote("sub_new"));
        TenantSecurityService security = mock(TenantSecurityService.class); when(security.isGlobalAdmin()).thenReturn(true);
        StripeLinkService links = new StripeLinkService(security, subscriptions, plans, stripe, webhooks, audit, environment, clock, resolver);
        assertThrows(IllegalArgumentException.class, () -> links.link(1L, "sub_old", "cus_one", "review"));
        links.link(1L, "sub_new", "cus_one", "review");
        assertEquals(SubscriptionStatus.ACTIVE, s.getStatus()); assertNull(s.getRetentionEndsAt());
        lifecycle.transition(s, SubscriptionStatus.CANCELLED, clock.instant().minus(Duration.ofDays(30)), false, "review");
        assertThrows(IllegalArgumentException.class, () -> links.link(1L, "sub_old", "cus_one", "review"));
    }
    @Test void paidRecoveryWithoutSavedCardRestoresAccess() throws Exception {
        TenantSubscription s = local(); s.setStatus(SubscriptionStatus.SUSPENDED);
        s.setGraceEndsAt(clock.instant().plus(Duration.ofDays(1))); s.getTenant().setActive(false);
        ObjectNode current = remote("sub_old"); current.remove("default_payment_method");
        when(plans.findByCode("SMALL")).thenReturn(Optional.of(plan(2, 5, "50.00")));
        webhooks.sync(s, current, false, "review");
        assertEquals(SubscriptionStatus.ACTIVE, s.getStatus()); assertTrue(lifecycle.accessState(s).fullAccess());
    }
    @Test void invoiceTotalDisagreesWithSumOfRoundedLines() {
        TenantInvoice invoice = new TenantInvoice();
        for (int i = 0; i < 2; i++) {
            TenantInvoiceItem item = new TenantInvoiceItem(); item.setUnitPrice(new BigDecimal("0.03"));
            item.setTaxRate(new BigDecimal("15")); invoice.addItem(item);
        }
        BigDecimal lines = invoice.getItems().stream().map(TenantInvoiceItem::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("0.06"), lines); assertEquals(new BigDecimal("0.07"), invoice.getTotalAmount());
    }
}

