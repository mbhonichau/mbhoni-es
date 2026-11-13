package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnershipService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final RoleRepository roles;
    private final TenantGovernance governance;
    private final TenantSecurityService security;
    private final AuditLogService audit;
    public OwnershipService(TenantRepository tenants, UserRepository users, RoleRepository roles,
            TenantGovernance governance, TenantSecurityService security, AuditLogService audit) {
        this.tenants = tenants; this.users = users; this.roles = roles; this.governance = governance; this.security = security; this.audit = audit;
    }
    @Transactional
    public void transfer(Long tenantId, Long newOwnerId, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Ownership change reason required");
        Tenant tenant = tenants.lockById(tenantId).orElseThrow();
        if (!security.isGlobalAdmin()) governance.requireOwner(tenantId);
        User next = users.findByIdAndTenantId(newOwnerId, tenantId).filter(User::isActive).orElseThrow();
        Long previous = tenant.getOwnerUserId();
        if (newOwnerId.equals(previous)) return;
        Role ownerRole = roles.findByNameAndTenantId("ROLE_OWNER", tenantId).orElseGet(() -> {
            Role role = new Role(); role.setTenant(tenant); role.setName("ROLE_OWNER"); role.setSystemRole(true);
            role.setPermissions(roles.findByNameAndTenantIsNull("ROLE_TENANT_ADMIN").orElseThrow().getPermissions());
            return roles.save(role);
        });
        if (previous != null) {
            User old = users.findByIdAndTenantId(previous, tenantId).orElseThrow();
            old.getRoles().removeIf(r -> "ROLE_OWNER".equals(r.getName())); users.save(old);
        }
        next.getRoles().add(ownerRole); users.save(next); tenant.setOwnerUserId(newOwnerId); tenants.save(tenant);
        audit.logForTenant(tenantId, security.isGlobalAdmin() ? "PLATFORM_OWNERSHIP_OVERRIDE" : "OWNERSHIP_TRANSFER",
                "TENANT", String.valueOf(tenantId), previous + " -> " + newOwnerId + ": " + reason.trim());
    }
}
