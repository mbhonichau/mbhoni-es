package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeWebhookServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StripeSignatureVerifier verifier = mock(StripeSignatureVerifier.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final WebhookReceiptRepository receipts = mock(WebhookReceiptRepository.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);
    private final SubscriptionLifecycle lifecycle = new SubscriptionLifecycle(subscriptions, audit, clock, 7, 7, 30);
    private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    private final com.mbhoni_creative.adminservice.AuditLogService planAudit = mock(com.mbhoni_creative.adminservice.AuditLogService.class);
    private final org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment().withProperty("app.stripe.price-plans.price_123", "STANDARD");
    private final StripeWebhookService service = new StripeWebhookService(verifier, mapper, stripe, subscriptions, receipts, lifecycle, clock,
            new StripePlanResolver(plans, environment), planAudit, mock(StripeInvoiceService.class));
    private TenantSubscription local;
    private final byte[] event = "{\"id\":\"evt_123\",\"type\":\"invoice.payment_failed\",\"data\":{\"object\":{\"subscription\":\"sub_123\"}}}".getBytes(StandardCharsets.UTF_8);
    @BeforeEach void setup() {
        Tenant tenant = new Tenant(); tenant.setId(1L);
        local = new TenantSubscription(); local.setTenant(tenant); local.setStatus(SubscriptionStatus.ACTIVE);
        local.setStripeSubscriptionId("sub_123"); local.setStripeCustomerId("cus_123");
        SubscriptionPlan plan = new SubscriptionPlan(); org.springframework.test.util.ReflectionTestUtils.setField(plan, "id", 1L);
        plan.setCode("STANDARD"); plan.setActive(true); local.setPlan(plan);
        when(plans.findByCode("STANDARD")).thenReturn(Optional.of(plan));
        when(subscriptions.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(local));
        when(subscriptions.lockByTenantId(1L)).thenReturn(Optional.of(local));
    }
    private JsonNode remote(String status, String customer) throws Exception {
        return mapper.readTree("{\"id\":\"sub_123\",\"customer\":\"" + customer + "\",\"status\":\"" + status
                + "\",\"items\":{\"data\":[{\"price\":\"price_123\"}]},\"default_payment_method\":{\"type\":\"card\"},\"latest_invoice\":{\"status\":\"paid\"}}");
    }
    @Test void authenticatesBeforeReadingAnyTenantData() {
        doThrow(new IllegalArgumentException("bad signature")).when(verifier).verify(event, "bad");
        assertThrows(IllegalArgumentException.class, () -> service.receive(event, "bad"));
        verifyNoInteractions(subscriptions, stripe, receipts);
    }
    @Test void duplicateEventDoesNotFetchOrMutateSubscription() {
        when(receipts.existsById("evt_123")).thenReturn(true);
        service.receive(event, "valid");
        verifyNoInteractions(stripe); verify(subscriptions, never()).save(any());
    }
    @Test void delayedFailureUsesCurrentStripeStateInsteadOfStalePayload() throws Exception {
        when(stripe.subscription("sub_123")).thenReturn(remote("active", "cus_123"));
        service.receive(event, "valid");
        assertEquals(SubscriptionStatus.ACTIVE, local.getStatus());
        verify(receipts).save(any()); verifyNoInteractions(audit);
    }
    @Test void failedPaymentStartsGraceAndRecordsReceipt() throws Exception {
        when(stripe.subscription("sub_123")).thenReturn(remote("past_due", "cus_123"));
        service.receive(event, "valid");
        assertEquals(SubscriptionStatus.PAST_DUE, local.getStatus());
        assertEquals(clock.instant().plus(Duration.ofDays(7)), local.getGraceEndsAt());
        verify(receipts).save(any()); verify(audit).save(any());
    }
    @Test void verifiedPaidPriceChangeUpdatesLocalEntitlementsAndAudits() throws Exception {
        SubscriptionPlan upgrade = new SubscriptionPlan(); org.springframework.test.util.ReflectionTestUtils.setField(upgrade, "id", 2L);
        upgrade.setCode("PREMIUM"); upgrade.setActive(true);
        environment.setProperty("app.stripe.price-plans.price_123", "PREMIUM"); when(plans.findByCode("PREMIUM")).thenReturn(Optional.of(upgrade));
        when(stripe.subscription("sub_123")).thenReturn(remote("active", "cus_123"));
        service.receive(event, "valid"); assertSame(upgrade, local.getPlan());
        verify(planAudit).logForTenant(eq(1L), eq("STRIPE_PLAN_CHANGED"), eq("TENANT"), eq("1"), contains("PREMIUM"));
    }
    @Test void unmappedPriceFailsWithoutAcknowledgingOrChangingEntitlements() throws Exception {
        environment.setProperty("app.stripe.price-plans.price_123", "MISSING");
        when(stripe.subscription("sub_123")).thenReturn(remote("active", "cus_123"));
        assertThrows(IllegalArgumentException.class, () -> service.receive(event, "valid"));
        assertEquals("STANDARD", local.getPlan().getCode()); verify(receipts, never()).save(any());
    }
    @Test void unpaidUpgradeDoesNotGrantNewEntitlements() throws Exception {
        ObjectNode current = (ObjectNode) remote("active", "cus_123"); ((ObjectNode)current.get("latest_invoice")).put("status", "open");
        when(stripe.subscription("sub_123")).thenReturn(current); service.receive(event, "valid");
        assertEquals("STANDARD", local.getPlan().getCode()); verifyNoInteractions(plans, planAudit);
    }
    @Test void customerMismatchCannotChangeTenantStateOrRecordSuccess() throws Exception {
        when(stripe.subscription("sub_123")).thenReturn(remote("past_due", "cus_OTHER"));
        assertThrows(ResponseStatusException.class, () -> service.receive(event, "valid"));
        assertEquals(SubscriptionStatus.ACTIVE, local.getStatus());
        verify(receipts, never()).save(any()); verifyNoInteractions(audit);
    }
    @Test void stripeFailureIsPropagatedWithoutSuccessReceipt() {
        when(stripe.subscription("sub_123")).thenThrow(new IllegalStateException("network"));
        assertThrows(IllegalStateException.class, () -> service.receive(event, "valid"));
        verify(receipts, never()).save(any());
    }
    @Test void cancelledTenantCannotBeResurrectedByOldPaidEvent() throws Exception {
        local.setStatus(SubscriptionStatus.CANCELLED);
        when(stripe.subscription("sub_123")).thenReturn(remote("active", "cus_123"));
        service.receive(event, "valid");
        assertEquals(SubscriptionStatus.CANCELLED, local.getStatus());
        verifyNoInteractions(audit);
    }
    @Test void providerPaidTrialConvertsWithoutSavedCard() throws Exception {
        local.setStatus(SubscriptionStatus.TRIAL);
        ObjectNode current = (ObjectNode) remote("active", "cus_123"); current.remove("default_payment_method");
        when(stripe.subscription("sub_123")).thenReturn(current);
        service.receive(event, "valid");
        assertEquals(SubscriptionStatus.ACTIVE, local.getStatus());
    }
    @Test void cancellationDuringDunningPreservesBothGracePeriods() throws Exception {
        local.setStatus(SubscriptionStatus.PAST_DUE);
        Instant paymentDeadline = clock.instant().minusSeconds(1);
        local.setGraceEndsAt(paymentDeadline);
        JsonNode cancelled = remote("canceled", "cus_123");
        service.sync(local, cancelled, false, "cancellation");
        assertEquals(SubscriptionStatus.SUSPENDED, local.getStatus());
        Instant suspendedDeadline = paymentDeadline.plus(Duration.ofDays(7));
        assertEquals(suspendedDeadline, local.getGraceEndsAt());
        service.sync(local, cancelled, false, "repeated cancellation");
        assertEquals(SubscriptionStatus.SUSPENDED, local.getStatus());
        assertEquals(suspendedDeadline, local.getGraceEndsAt());
    }
}

