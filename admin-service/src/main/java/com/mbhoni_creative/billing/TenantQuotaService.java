package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantQuotaService {
    private final TenantQuotaRepository quotas;
    private final TenantRepository tenants;
    private final TenantSubscriptionRepository subscriptions;
    private final TenantSecurityService security;
    private final TenantAccessService access;
    private final AuditLogService audit;
    public TenantQuotaService(TenantQuotaRepository quotas, TenantRepository tenants, TenantSubscriptionRepository subscriptions,
            TenantSecurityService security, TenantAccessService access, AuditLogService audit) {
        this.quotas = quotas; this.tenants = tenants; this.subscriptions = subscriptions; this.security = security; this.access = access; this.audit = audit;
    }
    public TenantQuota get(Long tenantId) {
        access.requireAccess(tenantId);
        return quotas.findByTenantId(tenantId).orElseGet(() -> {
            TenantQuota defaults = new TenantQuota(); defaults.setTenant(tenants.findById(tenantId).orElseThrow());
            defaults.setMaxUsers(subscriptions.findByTenantId(tenantId).map(TenantSubscription::getPlan).map(SubscriptionPlan::getMaxUsers).orElse(null));
            defaults.setMaxStorageGb(null); defaults.setMaxApiRequestsPerMinute(null);
            return defaults; // Reading a quota must never create an override.
        });
    }
    @Transactional
    public TenantQuota override(Long tenantId, TenantQuota source, String reason) {
        if (!security.isGlobalAdmin()) throw new AccessDeniedException("Quota overrides require a platform administrator");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Quota override reason required");
        for (Integer value : new Integer[]{source.getMaxUsers(), source.getMaxApiRequestsPerMinute(), source.getMaxStorageGb()})
            if (value != null && value < 1) throw new IllegalArgumentException("Quota limits must be positive");
        Tenant tenant = tenants.lockById(tenantId).orElseThrow();
        TenantQuota target = quotas.findByTenantId(tenantId).orElseGet(TenantQuota::new); target.setTenant(tenant);
        target.setMaxUsers(source.getMaxUsers()); target.setMaxApiRequestsPerMinute(source.getMaxApiRequestsPerMinute());
        target.setMaxStorageGb(source.getMaxStorageGb()); target.setCustomQuotasJson(source.getCustomQuotasJson());
        quotas.save(target);
        audit.logForTenant(tenantId, "QUOTA_OVERRIDE", "TENANT", String.valueOf(tenantId), reason.trim());
        return target;
    }
}
