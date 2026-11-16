package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class BillingRecoveryController {
    private final TenantSecurityService security;
    private final TenantGovernance governance;
    private final TenantSubscriptionRepository subscriptions;
    private final SubscriptionLifecycle lifecycle;
    private final StripeGateway stripe;
    private final AuditLogService audit;
    private final String returnUrl, configuration;
    public BillingRecoveryController(TenantSecurityService security, TenantGovernance governance, TenantSubscriptionRepository subscriptions,
            SubscriptionLifecycle lifecycle, StripeGateway stripe, AuditLogService audit,
            @Value("${app.stripe.portal-return-url:}") String returnUrl, @Value("${app.stripe.portal-configuration:}") String configuration) {
        this.security = security; this.governance = governance; this.subscriptions = subscriptions; this.lifecycle = lifecycle;
        this.stripe = stripe; this.audit = audit; this.returnUrl = returnUrl; this.configuration = configuration;
    }
    @GetMapping({"/account/state", "/account/billing"})
    public String status(Model model, jakarta.servlet.http.HttpServletRequest request) {
        Long tenantId = security.getCurrentTenantId();
        if (tenantId == null) throw new org.springframework.security.access.AccessDeniedException("Tenant context required");
        if (request.getRequestURI().endsWith("/billing")) governance.requireOwner(tenantId);
        TenantSubscription sub = subscriptions.findByTenantId(tenantId).orElse(null);
        var state = lifecycle.accessState(sub);
        model.addAttribute("state", state); model.addAttribute("owner", governance.isOwner(tenantId));
        model.addAttribute("linked", sub != null && sub.getStripeCustomerId() != null);
        return "account/state";
    }
    @PostMapping("/account/billing/portal")
    public String portal() {
        Long tenantId = security.getCurrentTenantId(); governance.requireOwner(tenantId);
        TenantSubscription sub = subscriptions.findByTenantId(tenantId).orElseThrow();
        if (lifecycle.accessState(sub).status() == SubscriptionStatus.CANCELLED)
            throw new org.springframework.security.access.AccessDeniedException("Cancelled tenants require fresh onboarding");
        String portal = stripe.portal(sub.getStripeCustomerId(), returnUrl, configuration);
        audit.logForTenant(tenantId, "BILLING_PORTAL_OPENED", "TENANT", String.valueOf(tenantId), "Owner opened Stripe billing recovery");
        return "redirect:" + portal;
    }
}
