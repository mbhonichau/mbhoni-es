package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.TenantGovernance;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeInvoiceServiceTest {
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final TenantGovernance governance = mock(TenantGovernance.class);
    private final StripeInvoiceService service = new StripeInvoiceService(stripe, invoices, subscriptions, governance);
    private TenantSubscription subscription() {
        var s = new TenantSubscription(); var tenant = new Tenant(); tenant.setId(1L); s.setTenant(tenant);
        s.setStripeSubscriptionId("sub_one"); s.setStripeCustomerId("cus_one"); return s;
    }
    private ObjectNode invoice() throws Exception {
        return (ObjectNode) new ObjectMapper().readTree("""
            {"id":"in_one","customer":"cus_one","parent":{"subscription_details":{"subscription":"sub_one"}},
             "currency":"usd","status":"open","subtotal":1001,"total":1083,"amount_paid":0,"amount_remaining":583,
             "total_taxes":[{"amount":82}],"hosted_invoice_url":"https://invoice.stripe.com/i/test",
             "created":1791244800,"due_date":null,"billing_reason":"subscription_update"}
            """);
    }
    @Test void copiesProviderProrationTaxAndCreditAdjustedBalanceWithoutRecalculation() throws Exception {
        var remote = invoice(); service.copy(subscription(), remote);
        var capture = org.mockito.ArgumentCaptor.forClass(Invoice.class); verify(invoices).save(capture.capture());
        var local = capture.getValue();
        assertEquals(new BigDecimal("10.01"), local.getSubtotal());
        assertEquals(new BigDecimal("0.82"), local.getTaxAmount());
        assertEquals(new BigDecimal("10.83"), local.getTotalAmount());
        assertEquals(new BigDecimal("5.83"), local.getBalanceDue());
        assertEquals("USD", local.getCurrency()); assertEquals(remote.toString(), local.getStripeInvoiceJson());
    }
    @Test void duplicateImportUpdatesExistingInvoiceRatherThanCreatingAnother() throws Exception {
        var existing = new Invoice(); existing.setTenant(subscription().getTenant());
        when(invoices.findByStripeInvoiceId("in_one")).thenReturn(Optional.of(existing));
        service.copy(subscription(), invoice()); service.copy(subscription(), invoice());
        verify(invoices, times(2)).save(same(existing));
    }
    @Test void rejectsCrossTenantProviderInvoiceBeforeWriting() throws Exception {
        var remote = invoice(); remote.put("customer", "cus_other");
        assertThrows(IllegalArgumentException.class, () -> service.copy(subscription(), remote)); verifyNoInteractions(invoices);
    }
    @Test void rejectsLocalInvoiceAlreadyAssignedToAnotherTenant() throws Exception {
        var existing = new Invoice(); var tenant = new Tenant(); tenant.setId(2L); existing.setTenant(tenant);
        when(invoices.findByStripeInvoiceId("in_one")).thenReturn(Optional.of(existing));
        assertThrows(IllegalStateException.class, () -> service.copy(subscription(), invoice())); verify(invoices, never()).save(any());
    }
    @Test void acceptsNegativeProrationCreditAndPreservesExactCents() throws Exception {
        var remote = invoice(); remote.put("subtotal", -1001).put("total", -1001).put("amount_remaining", 0);
        remote.set("total_taxes", new ObjectMapper().createArrayNode()); service.copy(subscription(), remote);
        var capture = org.mockito.ArgumentCaptor.forClass(Invoice.class); verify(invoices).save(capture.capture());
        assertEquals(new BigDecimal("-10.01"), capture.getValue().getTotalAmount());
        assertEquals(new BigDecimal("0.00"), capture.getValue().getBalanceDue());
    }
    @Test void rejectsUnsupportedCurrencyAndFractionalMinorUnits() throws Exception {
        var remote = invoice(); remote.put("currency", "jpy");
        assertThrows(IllegalArgumentException.class, () -> service.copy(subscription(), remote));
        remote.put("currency", "usd").put("total", 1.25);
        assertThrows(IllegalArgumentException.class, () -> service.copy(subscription(), remote)); verify(invoices, never()).save(any());
    }
    @Test void refreshRequiresOwnerBeforeProviderAccess() {
        doThrow(new org.springframework.security.access.AccessDeniedException("Owner required")).when(governance).requireBillingOwner(1L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.refresh(1L));
        verifyNoInteractions(stripe, subscriptions, invoices);
    }
}
