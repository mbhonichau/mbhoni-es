package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantBookkeepingIsolationTest {
    private final TenantInvoiceRepository invoices = mock(TenantInvoiceRepository.class);
    private final TenantQuotationRepository quotes = mock(TenantQuotationRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantInvoicingServiceImpl service = new TenantInvoicingServiceImpl(invoices, quotes, tenants,
            new TenantGovernance(tenants, new TenantSecurityService()));
    private Tenant own, other;
    private User caller;
    @BeforeEach void setup() {
        own = new Tenant(); own.setId(1L); own.setOwnerUserId(7L);
        other = new Tenant(); other.setId(2L); other.setOwnerUserId(8L);
        caller = new User(); caller.setTenant(own); caller.setId(7L);
        var principal = new CustomUserPrincipal(caller);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(tenants.findById(1L)).thenReturn(Optional.of(own));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void foreignInvoiceCannotBeReadDeletedOrReassigned() {
        TenantInvoice stored = new TenantInvoice(); stored.setTenant(other);
        when(invoices.findById(42L)).thenReturn(Optional.of(stored));
        TenantInvoice submitted = new TenantInvoice(); org.springframework.test.util.ReflectionTestUtils.setField(submitted, "id", 42L);
        assertThrows(AccessDeniedException.class, () -> service.getInvoiceById(42L));
        assertThrows(AccessDeniedException.class, () -> service.deleteInvoice(42L));
        assertThrows(AccessDeniedException.class, () -> service.saveInvoice(1L, submitted, null, null, null));
        verify(invoices, never()).save(any()); verify(invoices, never()).deleteById(any());
    }
    @Test void foreignQuotationCannotBeConvertedOrReassigned() {
        TenantQuotation stored = new TenantQuotation(); stored.setTenant(other);
        when(quotes.findById(42L)).thenReturn(Optional.of(stored));
        TenantQuotation submitted = new TenantQuotation(); org.springframework.test.util.ReflectionTestUtils.setField(submitted, "id", 42L);
        assertThrows(AccessDeniedException.class, () -> service.convertQuotationToInvoice(42L));
        assertThrows(AccessDeniedException.class, () -> service.saveQuotation(1L, submitted, null, null, null));
        verify(invoices, never()).save(any()); verify(quotes, never()).save(any());
    }
    @Test void tenantMemberWithBillingPermissionStillCannotReadBookkeeping() {
        caller.setId(99L);
        assertThrows(AccessDeniedException.class, () -> service.getInvoicesForTenant(1L));
        verifyNoInteractions(invoices);
    }
}
