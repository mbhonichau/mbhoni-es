package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.config.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantLifecycleFilter extends OncePerRequestFilter {
    private final TenantSubscriptionRepository subscriptions;
    private final SubscriptionLifecycle lifecycle;
    private final TenantGovernance governance;
    private final Clock clock;
    public TenantLifecycleFilter(TenantSubscriptionRepository subscriptions, SubscriptionLifecycle lifecycle, TenantGovernance governance, Clock clock) {
        this.subscriptions = subscriptions; this.lifecycle = lifecycle; this.governance = governance; this.clock = clock;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()
                || path.equals("/api/webhooks/stripe") || path.startsWith("/assets/") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/vendor/") || path.startsWith("/images/")) {
            chain.doFilter(request, response); return;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserPrincipal user && user.isGlobalAdmin()) { chain.doFilter(request, response); return; }
        Long tenantId = principal instanceof CustomUserPrincipal user ? user.getTenantId()
                : principal instanceof ApiKeyPrincipal key ? key.getTenantId() : null;
        if (tenantId == null) { deny(response, "Tenant context is required"); return; }
        var state = lifecycle.accessState(subscriptions.findByTenantId(tenantId).orElse(null));
        boolean owner = principal instanceof CustomUserPrincipal && governance.isOwner(tenantId);
        boolean bookkeeping = path.equals("/billing") || path.startsWith("/billing/")
                || path.equals("/tenant/invoices") || path.startsWith("/tenant/invoices/")
                || path.equals("/tenant/quotations") || path.startsWith("/tenant/quotations/");
        if (bookkeeping && !owner) { deny(response, "Billing and bookkeeping require the tenant Owner"); return; }
        if (state.fullAccess()) {
            if (state.status() == SubscriptionStatus.PAST_DUE) {
                request.setAttribute("billingWarning", "Payment is overdue. Please update billing before the grace period ends.");
                response.setHeader("X-Tenant-Billing-State", "PAST_DUE");
            }
            chain.doFilter(request, response); return;
        }
        boolean human = principal instanceof CustomUserPrincipal;
        boolean statePage = human && path.equals("/account/state") && request.getMethod().equals("GET");
        boolean recovery = owner && state.status() == SubscriptionStatus.SUSPENDED && (
                path.equals("/account/billing") && request.getMethod().equals("GET")
                || path.equals("/account/billing/portal") && request.getMethod().equals("POST"));
        boolean export = owner && state.status() == SubscriptionStatus.CANCELLED && state.retentionEndsAt() != null
                && clock.instant().isBefore(state.retentionEndsAt()) && path.equals("/account/export") && request.getMethod().equals("GET");
        if (statePage || recovery || export) { chain.doFilter(request, response); return; }
        deny(response, "Tenant is " + state.status() + ". See /account/state for recovery options.");
    }
    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403); response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
