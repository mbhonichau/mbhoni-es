package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.OrganizationMembershipRepository;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAccessService {
    private final OrganizationMembershipRepository memberships;
    private final TenantGovernance governance;
    private final TenantSecurityService security;
    public OrganizationAccessService(OrganizationMembershipRepository memberships, TenantGovernance governance, TenantSecurityService security) {
        this.memberships = memberships; this.governance = governance; this.security = security;
    }
    public boolean canAccess(OrganizationUnit unit) {
        if (security.isGlobalAdmin()) return true;
        if (unit == null) return true; // Unassigned records remain tenant-scoped by their calling service.
        Long tenantId = unit.getTenant() == null ? null : unit.getTenant().getId();
        if (tenantId == null || !tenantId.equals(security.getCurrentTenantId())) return false;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal p ? p : null;
        if (principal != null && governance.isOwner(tenantId)) return true;
        List<OrganizationMembership> assignments = principal == null || principal.getUser().getId() == null ? List.of()
                : memberships.findByUserIdAndUnitTenantId(principal.getUser().getId(), tenantId);
        Set<Long> visited = new HashSet<>();
        for (OrganizationUnit cursor = unit; cursor != null; cursor = cursor.getParentUnit()) {
            if (!visited.add(cursor.getId()) || cursor.getTenant() == null || !tenantId.equals(cursor.getTenant().getId())) return false;
            if (!cursor.isIsolated()) continue;
            OrganizationUnit boundary = cursor;
            boolean allowed = assignments.stream().filter(m -> m.getUser().getTenant() != null && tenantId.equals(m.getUser().getTenant().getId()))
                    .anyMatch(m -> descendantOf(m.getUnit(), boundary, tenantId)
                            || (!boundary.isRestrictParentVisibility() && m.isAdministrator() && descendantOf(boundary, m.getUnit(), tenantId)));
            if (!allowed) return false;
        }
        return true;
    }
    public void requireAccess(OrganizationUnit unit) {
        if (!canAccess(unit)) throw new AccessDeniedException("Organization membership required");
    }
    public static boolean descendantOf(OrganizationUnit node, OrganizationUnit ancestor, Long tenantId) {
        Set<Long> visited = new HashSet<>();
        for (OrganizationUnit cursor = node; cursor != null; cursor = cursor.getParentUnit()) {
            if (!visited.add(cursor.getId()) || cursor.getTenant() == null || !tenantId.equals(cursor.getTenant().getId())) return false;
            if (cursor.getId() != null && cursor.getId().equals(ancestor.getId())) return true;
        }
        return false;
    }
}
