package com.mbhoni_creative.adminservice.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.admindto.NavigationItem;
import com.mbhoni_creative.admindto.NavigationSection;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.NavigationService;
import com.mbhoni_creative.config.TenantEntitlementService;
import com.mbhoni_creative.config.TenantSecurityService;

import java.util.HashSet;
import java.util.Set;

import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminrepository.UserRepository;

@Service
public class NavigationServiceImpl implements NavigationService {

    private final ModuleService moduleService;
    private final TenantSecurityService tenantSecurityService;
    private final TenantEntitlementService tenantEntitlementService;
    private final UserRepository userRepository;

    public NavigationServiceImpl(
            ModuleService moduleService,
            TenantSecurityService tenantSecurityService,
            TenantEntitlementService tenantEntitlementService,
            UserRepository userRepository) {
        this.moduleService = moduleService;
        this.tenantSecurityService = tenantSecurityService;
        this.tenantEntitlementService = tenantEntitlementService;
        this.userRepository = userRepository;
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
                guardedItem("Tenants", "/tenants", "bi-buildings", null, requestPath, "TENANT_VIEW", "TENANT_EDIT", "ROLE_ADMIN"),
                guardedItem("Subscriptions", "/subscriptions/tenants", "bi-receipt", null, requestPath, "SUBSCRIPTION_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                globalAdminItem("Plans", "/subscriptions/plans", "bi-credit-card", null, requestPath, "SUBSCRIPTION_VIEW", "ROLE_ADMIN"),
                guardedItem("Billing Invoices", "/billing", "bi-cash-coin", null, requestPath, "BILLING_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                guardedItem("Expenses & Ledger", "/billing/expenses", "bi-wallet2", null, requestPath, "BILLING_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        addSection(sections, "Access", "bi-shield-check", candidates(
                guardedItem("Users", "/users", "bi-people", null, requestPath, "USER_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                guardedItem("Roles", "/roles", "bi-shield-lock", null, requestPath, "ROLE_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                featureItem("API Keys", "/api-keys", "bi-key", "API_ACCESS", requestPath, "API_KEY_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        addSection(sections, "Tenant Setup", "bi-sliders", candidates(
                globalAdminItem("Modules", "/modules", "bi-grid", null, requestPath, "MODULE_VIEW", "ROLE_ADMIN"),
                globalAdminItem("Customizations", "/customizations", "bi-palette", null, requestPath, "CUSTOMIZATION_VIEW", "ROLE_ADMIN"),
                featureTenantItem("Branding", "/customizations/my", "bi-palette", "BRANDING", requestPath, "CUSTOMIZATION_VIEW", "ROLE_TENANT_ADMIN"),
                globalAdminItem("Quotas", "/tenants/quotas", "bi-speedometer", null, requestPath, "TENANT_EDIT", "ROLE_ADMIN")
        ));

        addSection(sections, "Business Modules", "bi-briefcase", candidates(
                moduleItem("Organization", "/organization", "bi-diagram-2", "ORG", requestPath, "ORG_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Employee Roster", "/employees", "bi-person-vcard", "EMPLOYEE", requestPath, "EMPLOYEE_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Field Configurator", "/employee-fields", "bi-sliders", "EMPLOYEE", requestPath, "EMPLOYEE_FIELD_SCHEMA_MANAGE", "EMPLOYEE_FIELD_SCHEMA_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Completeness Report", "/employees/completeness-report", "bi-clipboard-check", "EMPLOYEE", requestPath, "EMPLOYEE_COMPLETENESS_VIEW", "EMPLOYEE_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Payroll & Payslips", "/employees/payslips", "bi-calculator", "EMPLOYEE", requestPath, "EMPLOYEE_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                guardedItem("Invoices & Billing", "/tenant/invoices", "bi-receipt-cutoff", null, requestPath, "BILLING_VIEW", "SERVICE_VIEW", "CONTRACT_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_USER"),
                guardedItem("Quotations & Estimates", "/tenant/quotations", "bi-file-earmark-spreadsheet", null, requestPath, "BILLING_VIEW", "SERVICE_VIEW", "CONTRACT_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_USER"),
                moduleItem("Contracts", "/contracts", "bi-file-earmark-text", "CONTRACT", requestPath, "CONTRACT_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Master Data", "/master-data", "bi-database-gear", "MASTER_DATA", requestPath, "MASTER_DATA_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                moduleItem("Services", "/services", "bi-diagram-3", "SERVICE", requestPath, "SERVICE_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN")
        ));

        addSection(sections, "Governance", "bi-clipboard-check", candidates(
                guardedItem("Audit Logs", "/audit-logs", "bi-clipboard-data", null, requestPath, "AUDIT_VIEW", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"),
                globalAdminItem("System Status", "/system", "bi-cpu", null, requestPath, "AUDIT_VIEW", "ROLE_ADMIN")
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

    private NavigationItem featureItem(
            String label,
            String url,
            String icon,
            String featureName,
            String requestPath,
            String... anyAuthority) {

        if (!hasAnyAuthority(anyAuthority)) {
            return null;
        }

        if (!tenantEntitlementService.isFeatureEnabled(featureName)) {
            return null;
        }

        return item(label, url, icon, null, requestPath);
    }

    private NavigationItem featureTenantItem(
            String label,
            String url,
            String icon,
            String featureName,
            String requestPath,
            String... anyAuthority) {

        if (tenantSecurityService.isGlobalAdmin()) {
            return null;
        }

        return featureItem(label, url, icon, featureName, requestPath, anyAuthority);
    }

    private NavigationItem globalAdminItem(
            String label,
            String url,
            String icon,
            String badge,
            String requestPath,
            String... anyAuthority) {

        return tenantSecurityService.isGlobalAdmin() && hasAnyAuthority(anyAuthority)
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

        if (!tenantEntitlementService.isModuleEnabled(moduleCode)) {
            return null;
        }

        return item(label, url, icon, null, requestPath);
    }

    private NavigationItem item(String label, String url, String icon, String badge, String requestPath) {
        return new NavigationItem(label, url, icon, badge, isActive(url, requestPath));
    }

    private boolean hasAnyAuthority(String... authorityNames) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        Set<String> liveAuthorities = getLiveAuthorities(authentication);

        for (String authorityName : authorityNames) {
            if (liveAuthorities.contains(authorityName)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> getLiveAuthorities(Authentication authentication) {
        Set<String> authorities = new HashSet<>();

        for (GrantedAuthority ga : authentication.getAuthorities()) {
            authorities.add(ga.getAuthority());
        }

        String username = authentication.getName();
        if (username != null && !"anonymousUser".equals(username)) {
            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.isGlobalAdmin()) {
                    authorities.add("ROLE_ADMIN");
                }
                if (user.getRoles() != null) {
                    for (Role role : user.getRoles()) {
                        authorities.add(role.getName());
                        if (role.getPermissions() != null) {
                            for (Permission permission : role.getPermissions()) {
                                authorities.add(permission.getName());
                            }
                        }
                    }
                }
            });
        }

        return authorities;
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
