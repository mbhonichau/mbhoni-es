package com.mbhoni_creative.adminservice.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.admindto.NavigationItem;
import com.mbhoni_creative.admindto.NavigationSection;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.NavigationService;
import com.mbhoni_creative.config.TenantSecurityService;

@Service
public class NavigationServiceImpl implements NavigationService {

    private final ModuleService moduleService;
    private final TenantSecurityService tenantSecurityService;

    public NavigationServiceImpl(ModuleService moduleService, TenantSecurityService tenantSecurityService) {
        this.moduleService = moduleService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    public List<NavigationSection> getSidebarSections(String requestPath) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return List.of();
        }

        List<NavigationSection> sections = new ArrayList<>();

        addSection(sections, "Workspace", "bi-compass", List.of(
                item("Dashboard", "/dashboard", "bi-speedometer2", null, requestPath)
        ));

        addSection(sections, "Platform", "bi-buildings", candidates(
                guardedItem("Tenants", "/tenants", "bi-buildings", null, requestPath, "TENANT_VIEW"),
                guardedItem("Subscriptions", "/subscriptions/tenants", "bi-receipt", null, requestPath, "SUBSCRIPTION_VIEW"),
                guardedItem("Plans", "/subscriptions/plans", "bi-credit-card", null, requestPath, "SUBSCRIPTION_VIEW"),
                guardedItem("Billing", "/billing", "bi-cash-coin", null, requestPath, "BILLING_VIEW")
        ));

        addSection(sections, "Access", "bi-shield-check", candidates(
                guardedItem("Users", "/users", "bi-people", null, requestPath, "USER_VIEW"),
                guardedItem("Roles", "/roles", "bi-shield-lock", null, requestPath, "ROLE_VIEW"),
                guardedItem("API Keys", "/api-keys", "bi-key", null, requestPath, "API_KEY_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        addSection(sections, "Tenant Setup", "bi-sliders", candidates(
                guardedItem("Modules", "/modules", "bi-grid", null, requestPath, "MODULE_VIEW"),
                guardedItem("Customizations", "/customizations", "bi-palette", null, requestPath, "TENANT_VIEW", "CUSTOMIZATION_VIEW"),
                tenantItem("Branding", "/customizations/my", "bi-palette", null, requestPath, "CUSTOMIZATION_VIEW"),
                guardedItem("Quotas", "/tenants/quotas", "bi-speedometer", null, requestPath, "TENANT_EDIT", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        addSection(sections, "Business Modules", "bi-briefcase", candidates(
                moduleItem("Organization", "/organization", "bi-diagram-2", "ORG", requestPath, "ORG_VIEW", "TENANT_VIEW"),
                moduleItem("Employees", "/employees", "bi-person-vcard", "EMPLOYEE", requestPath, "EMPLOYEE_VIEW", "TENANT_VIEW"),
                moduleItem("Contracts", "/contracts", "bi-file-earmark-text", "CONTRACT", requestPath, "CONTRACT_VIEW", "TENANT_VIEW"),
                moduleItem("Master Data", "/master-data", "bi-database-gear", "MASTER_DATA", requestPath, "MASTER_DATA_VIEW", "TENANT_VIEW"),
                moduleItem("Services", "/services", "bi-diagram-3", "SERVICE", requestPath, "SERVICE_VIEW", "TENANT_VIEW")
        ));

        addSection(sections, "Governance", "bi-clipboard-check", candidates(
                guardedItem("Audit Logs", "/audit-logs", "bi-clipboard-data", null, requestPath, "AUDIT_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        return sections;
    }

    private List<NavigationItem> candidates(NavigationItem... items) {
        return Arrays.asList(items);
    }

    private void addSection(List<NavigationSection> sections, String title, String icon, List<NavigationItem> candidates) {
        List<NavigationItem> visibleItems = candidates.stream()
                .filter(item -> item != null)
                .toList();

        if (!visibleItems.isEmpty()) {
            sections.add(new NavigationSection(title, icon, visibleItems));
        }
    }

    private NavigationItem guardedItem(
            String label,
            String url,
            String icon,
            String badge,
            String requestPath,
            String... anyAuthority) {

        return hasAnyAuthority(anyAuthority)
                ? item(label, url, icon, badge, requestPath)
                : null;
    }

    private NavigationItem tenantItem(
            String label,
            String url,
            String icon,
            String badge,
            String requestPath,
            String... anyAuthority) {

        return !tenantSecurityService.isGlobalAdmin() && hasAnyAuthority(anyAuthority)
                ? item(label, url, icon, badge, requestPath)
                : null;
    }

    private NavigationItem moduleItem(
            String label,
            String url,
            String icon,
            String moduleCode,
            String requestPath,
            String... anyAuthority) {

        if (!hasAnyAuthority(anyAuthority)) {
            return null;
        }

        if (!tenantSecurityService.isGlobalAdmin() && !isEnabledForCurrentTenant(moduleCode)) {
            return null;
        }

        return item(label, url, icon, null, requestPath);
    }

    private NavigationItem item(String label, String url, String icon, String badge, String requestPath) {
        return new NavigationItem(label, url, icon, badge, isActive(url, requestPath));
    }

    private boolean isEnabledForCurrentTenant(String moduleCode) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();

        if (tenantId == null) {
            return false;
        }

        return moduleService.isModuleEnabled(tenantId, moduleCode);
    }

    private boolean hasAnyAuthority(String... authorityNames) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (String authorityName : authorityNames) {
            boolean match = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authorityName::equals);

            if (match) {
                return true;
            }
        }

        return false;
    }

    private boolean isActive(String url, String requestPath) {
        if (requestPath == null) {
            return false;
        }

        if ("/dashboard".equals(url)) {
            return "/dashboard".equals(requestPath) || "/".equals(requestPath);
        }

        return requestPath.equals(url) || requestPath.startsWith(url + "/");
    }
}
