package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSignupService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final RoleRepository roles;
    private final SubscriptionPlanRepository plans;
    private final TenantSubscriptionRepository subscriptions;
    private final TenantSettingsRepository settings;
    private final TenantCustomizationRepository customizations;
    private final BillingAccountRepository accounts;
    private final SubscriptionPlanModuleRepository planModules;
    private final TenantModuleRepository tenantModules;
    private final AuditLogRepository audit;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final int trialDays;
    public TenantSignupService(TenantRepository tenants, UserRepository users, RoleRepository roles,
            SubscriptionPlanRepository plans, TenantSubscriptionRepository subscriptions, TenantSettingsRepository settings,
            TenantCustomizationRepository customizations, BillingAccountRepository accounts,
            SubscriptionPlanModuleRepository planModules, TenantModuleRepository tenantModules,
            AuditLogRepository audit, PasswordEncoder passwords, Clock clock,
            @Value("${app.lifecycle.trial-days:14}") int trialDays) {
        if (trialDays < 1) throw new IllegalArgumentException("Trial duration must be positive");
        this.tenants = tenants; this.users = users; this.roles = roles; this.plans = plans;
        this.subscriptions = subscriptions; this.settings = settings; this.customizations = customizations;
        this.accounts = accounts; this.planModules = planModules; this.tenantModules = tenantModules;
        this.audit = audit; this.passwords = passwords; this.clock = clock; this.trialDays = trialDays;
    }
    @Transactional
    public User signup(String organization, String username, String email, String password) {
        if (password == null || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must be at least 12 characters and at most 72 UTF-8 bytes");
        return provision(organization, username, email, passwords.encode(password), "LOCAL", null);
    }
    @Transactional
    public User signupGoogle(String email, String subject, String name) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Verified Google subject required");
        User user = provision((name == null || name.isBlank() ? "My" : name) + " workspace", email, email,
                passwords.encode(UUID.randomUUID().toString()), "GOOGLE", subject);
        user.setFirstName(name);
        return user;
    }
    private User provision(String organization, String username, String email, String encodedPassword, String provider, String subject) {
        if (organization == null || organization.isBlank() || organization.length() > 160)
            throw new IllegalArgumentException("Organization name is required (maximum 160 characters)");
        if (username == null || username.isBlank() || username.length() > 160 || email == null
                || email.length() > 160 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Valid username and email required");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username.trim()) || users.existsByEmail(normalizedEmail))
            throw new IllegalArgumentException("An account with these details already exists; sign in instead");
        SubscriptionPlan plan = plans.findByCode(TrialPlanInitializer.CODE).filter(SubscriptionPlan::isActive).orElseThrow();
        if (plan.getMaxUsers() == null || plan.getMaxUsers() < 1) throw new IllegalStateException("Trial requires a seat cap");
        Role adminTemplate = roles.findByNameAndTenantIsNull("ROLE_TENANT_ADMIN").orElseThrow();
        Role memberTemplate = roles.findByNameAndTenantIsNull("ROLE_USER").orElseThrow();
        Tenant tenant = new Tenant(); tenant.setName(organization.trim()); tenant.setTenantCode(UUID.randomUUID().toString());
        tenant.setStatus("TRIAL"); tenant.setActive(true); tenants.save(tenant);
        Role owner = createRole(tenant, "ROLE_OWNER", adminTemplate.getPermissions());
        createRole(tenant, "ROLE_TENANT_ADMIN", adminTemplate.getPermissions());
        createRole(tenant, "ROLE_USER", memberTemplate.getPermissions());
        User user = new User(); user.setUsername(username.trim()); user.setEmail(normalizedEmail); user.setPassword(encodedPassword);
        user.setTenant(tenant); user.setRoles(Set.of(owner)); user.setAuthProvider(provider); user.setProviderId(subject);
        user.setGlobalAdmin(false); user.setActive(true); users.save(user);
        tenant.setOwnerUserId(user.getId()); tenants.save(tenant);
        TenantSubscription sub = new TenantSubscription(); sub.setTenant(tenant); sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.TRIAL); sub.setStartDate(LocalDate.now(clock));
        sub.setTrialEndsAt(LocalDate.now(clock).plusDays(trialDays)); sub.setStateChangedAt(clock.instant()); sub.setAutoRenew(false);
        subscriptions.save(sub);
        TenantSettings defaults = new TenantSettings(); defaults.setTenant(tenant); defaults.setSelfRegistrationAllowed(false);
        defaults.setAutoRenewalEnabled(false); settings.save(defaults);
        TenantCustomization branding = new TenantCustomization(); branding.setTenant(tenant); branding.setDisplayName(organization.trim());
        customizations.save(branding);
        BillingAccount billing = new BillingAccount(); billing.setTenant(tenant); billing.setBillingName(organization.trim());
        billing.setBillingEmail(normalizedEmail); accounts.save(billing);
        for (SubscriptionPlanModule mapping : planModules.findByPlanAndAllowedTrue(plan)) {
            TenantModule module = new TenantModule(); module.setTenant(tenant); module.setModule(mapping.getModule());
            module.setEnabled(true); tenantModules.save(module);
        }
        AuditLog log = new AuditLog(); log.setTenantId(tenant.getId()); log.setUsername(user.getUsername());
        log.setActorType("SIGNUP"); log.setAction("TENANT_PROVISIONED"); log.setResourceType("TENANT");
        log.setResourceId(String.valueOf(tenant.getId())); log.setDescription("Atomic trial signup with explicit Owner");
        log.setSuccess(true); log.setOccurredAt(LocalDateTime.now(clock)); audit.save(log);
        return user;
    }
    private Role createRole(Tenant tenant, String name, Set<Permission> permissions) {
        Role role = new Role(); role.setTenant(tenant); role.setName(name); role.setSystemRole(true);
        role.setPermissions(permissions); return roles.save(role);
    }
}
