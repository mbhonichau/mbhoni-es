package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationBoundaryService {
    private final OrganizationUnitRepository units;
    private final OrganizationMembershipRepository memberships;
    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantGovernance governance;
    private final TenantSecurityService security;
    private final AuditLogService audit;
    public OrganizationBoundaryService(OrganizationUnitRepository units, OrganizationMembershipRepository memberships, UserRepository users,
            TenantRepository tenants, TenantGovernance governance, TenantSecurityService security, AuditLogService audit) {
        this.units = units; this.memberships = memberships; this.users = users; this.tenants = tenants;
        this.governance = governance; this.security = security; this.audit = audit;
    }
    private OrganizationUnit authorized(Long unitId, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Security change reason required");
        OrganizationUnit unit = units.findById(unitId).orElseThrow();
        Long tenantId = unit.getTenant().getId();
        if (!security.isGlobalAdmin()) governance.requireOwner(tenantId);
        tenants.lockById(tenantId).orElseThrow(); return unit;
    }
    @Transactional
    public void boundary(Long unitId, boolean isolated, boolean restrictParents, String reason) {
        OrganizationUnit unit = authorized(unitId, reason);
        if (restrictParents && !isolated) throw new IllegalArgumentException("Parent restrictions require an isolated boundary");
        unit.setIsolated(isolated); unit.setRestrictParentVisibility(restrictParents); units.save(unit);
        audit.logForTenant(unit.getTenant().getId(), "ORG_BOUNDARY_CHANGED", "ORGANIZATION", String.valueOf(unitId),
                "isolated=" + isolated + ", restrictParents=" + restrictParents + ": " + reason.trim());
    }
    @Transactional
    public void membership(Long unitId, Long userId, boolean administrator, boolean remove, String reason) {
        OrganizationUnit unit = authorized(unitId, reason);
        User user = users.findByIdAndTenantId(userId, unit.getTenant().getId()).orElseThrow();
        OrganizationMembership member = memberships.findByUserIdAndUnitId(userId, unitId).orElseGet(OrganizationMembership::new);
        if (remove) { if (member.getId() != null) memberships.delete(member); }
        else { member.setUser(user); member.setUnit(unit); member.setAdministrator(administrator); memberships.save(member); }
        audit.logForTenant(unit.getTenant().getId(), "ORG_MEMBERSHIP_CHANGED", "ORGANIZATION", String.valueOf(unitId),
                "user=" + userId + ", admin=" + administrator + ", removed=" + remove + ": " + reason.trim());
    }
}
