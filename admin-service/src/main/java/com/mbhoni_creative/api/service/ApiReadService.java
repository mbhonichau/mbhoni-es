package com.mbhoni_creative.api.service;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.api.dto.*;
import com.mbhoni_creative.api.dto.ApiDtos.*;
import com.mbhoni_creative.api.mapper.ApiMapper;
import com.mbhoni_creative.api.security.ApiScopes;
import com.mbhoni_creative.billing.SubscriptionLifecycle;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import com.mbhoni_creative.security.TenantContext;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ApiReadService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final TenantSettingsRepository settings;
    private final TenantCustomizationRepository branding;
    private final TenantQuotaRepository quotas;
    private final TenantSubscriptionRepository subscriptions;
    private final TenantModuleRepository tenantModules;
    private final SubscriptionPlanModuleRepository planModules;
    private final ModuleService modules;
    private final SubscriptionLifecycle lifecycle;
    public ApiReadService(TenantRepository tenants, UserRepository users, TenantSettingsRepository settings,
            TenantCustomizationRepository branding, TenantQuotaRepository quotas, TenantSubscriptionRepository subscriptions,
            TenantModuleRepository tenantModules, SubscriptionPlanModuleRepository planModules,
            ModuleService modules, SubscriptionLifecycle lifecycle) {
        this.tenants = tenants; this.users = users; this.settings = settings; this.branding = branding; this.quotas = quotas;
        this.subscriptions = subscriptions; this.tenantModules = tenantModules; this.planModules = planModules;
        this.modules = modules; this.lifecycle = lifecycle;
    }
    private ApiKeyPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ApiKeyPrincipal key)
                || !Objects.equals(key.getTenantId(), TenantContext.getTenantId())) throw new AccessDeniedException("Tenant credential required");
        return key;
    }
    private Tenant tenant() {
        Long id = principal().getTenantId();
        if (id == null) throw new AccessDeniedException("Tenant credential required");
        Tenant tenant = tenants.findById(id).orElseThrow(() -> new AccessDeniedException("Tenant unavailable"));
        if (!tenant.isActive() || !lifecycle.accessState(subscriptions.findByTenantId(id).orElse(null)).fullAccess())
            throw new AccessDeniedException("Tenant subscription does not permit API access");
        return tenant;
    }
    public Identity me() {
        Tenant tenant = tenant(); var key = principal().getApiKey();
        Set<String> permissions = new TreeSet<>(); key.getPermissions().forEach(p -> permissions.add(p.getName()));
        return new Identity("API_KEY", key.getId(), key.getName(), tenant.getId(), Set.of(), permissions, ApiScopes.from(key.getPermissions()));
    }
    public UserProfile user(Long id) {
        Tenant tenant = tenant();
        User user = users.findByIdAndTenantId(id, tenant.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireValidRoles(user, tenant.getId()); return ApiMapper.user(user);
    }
    public AccessResult check(AccessCheck request) {
        Tenant tenant = tenant();
        var user = users.findByIdAndTenantId(request.userId(), tenant.getId()).orElse(null);
        if (user == null || !user.isActive()) return new AccessResult(false);
        requireValidRoles(user, tenant.getId());
        return new AccessResult(ApiMapper.permissions(user).contains(request.permission()));
    }
    private static void requireValidRoles(User user, Long tenantId) {
        if (user.getRoles().stream().anyMatch(r -> r.getTenant() != null && !tenantId.equals(r.getTenant().getId())))
            throw new AccessDeniedException("User role tenant mismatch");
    }
    public TenantProfile profile() {
        Tenant t = tenant();
        return new TenantProfile(t.getId(), t.getTenantCode(), t.getName(), t.getStatus(), t.isActive(),
                ApiMapper.settings(settings.findByTenantId(t.getId()).orElse(null)));
    }
    public Branding branding() {
        Tenant t = tenant(); var b = branding.findByTenantId(t.getId()).orElse(null);
        return b == null ? new Branding(t.getName(), null, "#2563eb", "#16a34a")
                : new Branding(b.getDisplayName(), b.getLogoUrl(), b.getPrimaryColor(), b.getSecondaryColor());
    }
    public Quotas quotas() {
        Tenant t = tenant(); var q = quotas.findByTenantId(t.getId()).orElse(null);
        var plan = subscriptions.findByTenantId(t.getId()).map(TenantSubscription::getPlan).orElse(null);
        Integer seats = q != null && q.getMaxUsers() != null ? q.getMaxUsers() : plan == null ? null : plan.getMaxUsers();
        return new Quotas(seats, users.countByTenantAndActiveTrue(t), q == null ? null : q.getMaxStorageGb(), null,
                q == null ? null : q.getMaxApiRequestsPerMinute(), null);
    }
    public ApiEnvelope<List<ApiDtos.Module>> modules(int page, int size, LocalDateTime updatedSince) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be non-negative and size must be 1..100");
        Tenant t = tenant(); var subscription = subscriptions.findByTenantId(t.getId()).orElse(null);
        var plan = subscription == null ? null : subscription.getPlan();
        Map<String, PlatformModule> catalog = new TreeMap<>();
        Map<String, List<BaseEntity>> parts = new HashMap<>();
        for (TenantModule assignment : tenantModules.findByTenantOrderByModuleCategoryAscModuleNameAsc(t)) {
            var module = assignment.getModule(); catalog.put(module.getCode(), module);
            parts.computeIfAbsent(module.getCode(), ignored -> new ArrayList<>()).add(assignment);
        }
        if (plan != null) for (SubscriptionPlanModule mapping : planModules.findByPlanAndAllowedTrue(plan)) {
            var module = mapping.getModule(); catalog.put(module.getCode(), module);
            parts.computeIfAbsent(module.getCode(), ignored -> new ArrayList<>()).add(mapping);
        }
        List<ApiDtos.Module> result = new ArrayList<>();
        for (var module : catalog.values()) {
            List<BaseEntity> entities = parts.get(module.getCode()); entities.add(module);
            if (subscription != null) entities.add(subscription);
            if (plan != null) { entities.add(plan); planModules.findByPlanAndModule(plan, module).ifPresent(entities::add); }
            LocalDateTime updated = entities.stream().anyMatch(e -> e.getUpdatedAt() == null) ? null
                    : entities.stream().map(BaseEntity::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);
            // Unknown timestamps are included, never silently lost from an incremental response.
            if (updatedSince == null || updated == null || updated.isAfter(updatedSince))
                result.add(new ApiDtos.Module(module.getCode(), module.getName(), module.isActive() && modules.isModuleEnabled(t.getId(), module.getCode()), updated));
        }
        int from = (int) Math.min((long) page * size, result.size());
        return new ApiEnvelope<>(List.copyOf(result.subList(from, Math.min(from + size, result.size()))),
                new ApiEnvelope.Page(page, size, result.size()));
    }
    public Entitlement entitlement(String code) {
        Tenant t = tenant(); String normalized = code.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]{1,80}")) throw new IllegalArgumentException("Invalid module code");
        var s = subscriptions.findByTenantId(t.getId()).orElse(null);
        Instant until = s == null ? null : s.getCurrentPeriodEnd();
        if (s != null && s.getStatus() == SubscriptionStatus.TRIAL && s.getTrialEndsAt() != null)
            until = s.getTrialEndsAt().atStartOfDay(ZoneOffset.UTC).toInstant();
        if (s != null && s.getStatus() == SubscriptionStatus.PAST_DUE) until = s.getGraceEndsAt();
        return new Entitlement(normalized, modules.isModuleEnabled(t.getId(), normalized),
                s == null || s.getPlan() == null ? null : s.getPlan().getCode(), until);
    }
}
