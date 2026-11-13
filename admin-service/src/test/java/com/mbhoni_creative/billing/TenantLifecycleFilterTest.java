package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantLifecycleFilterTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantGovernance governance = new TenantGovernance(tenants, new TenantSecurityService());
    private final SubscriptionLifecycle lifecycle = new SubscriptionLifecycle(subscriptions, mock(AuditLogRepository.class), clock, 7, 7, 30);
    private final TenantLifecycleFilter filter = new TenantLifecycleFilter(subscriptions, lifecycle, governance, clock);
    private Tenant tenant;
    private TenantSubscription subscription;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setOwnerUserId(7L);
        subscription = new TenantSubscription(); subscription.setTenant(tenant); subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        when(subscriptions.findByTenantId(1L)).thenReturn(Optional.of(subscription));
        user(7L);
    }
    private void user(Long id) {
        User user = new User(); user.setId(id); user.setTenant(tenant);
        var principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private MockHttpServletResponse request(String method, String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(method, path), response, (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));
        return response;
    }
    @Test void expiredTrialCannotUseBusinessEndpointsBeforeSchedulerRuns() throws Exception {
        subscription.setStatus(SubscriptionStatus.TRIAL); subscription.setTrialEndsAt(LocalDate.of(2026, 9, 26));
        assertEquals(403, request("GET", "/employees").getStatus());
        assertEquals(403, request("POST", "/api/employees").getStatus());
        assertEquals(204, request("GET", "/account/billing").getStatus());
    }
    @Test void suspendedOwnerGetsOnlyRecoveryRoutesNotBookkeeping() throws Exception {
        subscription.setStatus(SubscriptionStatus.SUSPENDED); subscription.setGraceEndsAt(clock.instant().plusSeconds(60));
        assertEquals(204, request("POST", "/account/billing/portal").getStatus());
        assertEquals(403, request("GET", "/billing/invoices").getStatus());
        assertEquals(403, request("GET", "/tenant/invoices").getStatus());
        user(8L); assertEquals(403, request("GET", "/account/billing").getStatus());
    }
    @Test void nonOwnerCannotUseAnyBookkeepingEvenWhenActive() throws Exception {
        user(8L);
        for (String path : List.of("/billing", "/billing/expenses/create", "/tenant/invoices", "/tenant/quotations/4/print"))
            assertEquals(403, request("GET", path).getStatus());
    }
    @Test void cancelledOwnerExportExpiresExactlyAtDeadline() throws Exception {
        subscription.setStatus(SubscriptionStatus.CANCELLED); subscription.setRetentionEndsAt(clock.instant().plusSeconds(1));
        assertEquals(204, request("GET", "/account/export").getStatus());
        assertEquals(403, request("POST", "/account/export").getStatus());
        subscription.setRetentionEndsAt(clock.instant()); assertEquals(403, request("GET", "/account/export").getStatus());
    }
    @Test void restrictedApiKeysCannotUseRecoveryOrExport() throws Exception {
        TenantApiKey key = new TenantApiKey(); key.setTenant(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(key), null, List.of()));
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        assertEquals(403, request("GET", "/account/billing").getStatus());
        assertEquals(403, request("GET", "/api/employees").getStatus());
    }
    @Test void paymentGraceKeepsAccessAndWarning() throws Exception {
        subscription.setStatus(SubscriptionStatus.PAST_DUE); subscription.setGraceEndsAt(clock.instant().plusSeconds(1));
        var response = request("GET", "/employees"); assertEquals(204, response.getStatus());
        assertEquals("PAST_DUE", response.getHeader("X-Tenant-Billing-State"));
        subscription.setGraceEndsAt(clock.instant()); assertEquals(403, request("GET", "/employees").getStatus());
    }
}
