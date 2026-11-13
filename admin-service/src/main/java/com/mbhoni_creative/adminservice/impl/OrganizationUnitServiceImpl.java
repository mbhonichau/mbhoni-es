package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.OrganizationUnitRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.OrganizationUnitService;
import com.mbhoni_creative.config.TenantAccessService;

@Service
@Transactional
public class OrganizationUnitServiceImpl implements OrganizationUnitService {

    private final OrganizationUnitRepository orgRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessService tenantAccessService;
    private final com.mbhoni_creative.config.OrganizationAccessService organizationAccess;
    private final com.mbhoni_creative.config.TenantGovernance governance;
    private final com.mbhoni_creative.config.TenantSecurityService security;

    public OrganizationUnitServiceImpl(
            OrganizationUnitRepository orgRepository,
            TenantRepository tenantRepository, TenantAccessService tenantAccessService,
            com.mbhoni_creative.config.OrganizationAccessService organizationAccess,
            com.mbhoni_creative.config.TenantGovernance governance, com.mbhoni_creative.config.TenantSecurityService security) {
        this.orgRepository = orgRepository;
        this.tenantRepository = tenantRepository;
        this.tenantAccessService = tenantAccessService;
        this.organizationAccess = organizationAccess; this.governance = governance; this.security = security;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationUnit> getOrganizationUnitsByTenant(Long tenantId) {
        tenantId = tenantAccessService.resolveAccessibleTenantId(tenantId);
        return (tenantId != null ? orgRepository.findByTenantId(tenantId) : orgRepository.findAll()).stream().filter(organizationAccess::canAccess).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationUnit> getRootOrganizationUnits(Long tenantId) {
        tenantAccessService.requireAccess(tenantId);
        return orgRepository.findByTenantIdAndParentUnitIsNull(tenantId).stream().filter(organizationAccess::canAccess).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationUnit getOrganizationUnitById(Long id) {
        OrganizationUnit unit = orgRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization Unit not found with ID: " + id));
        tenantAccessService.requireAccess(unit.getTenant() != null ? unit.getTenant().getId() : null);
        organizationAccess.requireAccess(unit);
        return unit;
    }

    @Override
    public OrganizationUnit saveOrganizationUnit(Long tenantId, Long parentUnitId, OrganizationUnit unit) {
        tenantAccessService.requireAccess(tenantId);
        Tenant tenant = tenantRepository.lockById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
        OrganizationUnit target = unit.getId() == null ? new OrganizationUnit() : getOrganizationUnitById(unit.getId());
        if (target.getTenant() != null && !tenantId.equals(target.getTenant().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Organization tenant cannot be reassigned");
        OrganizationUnit parent = parentUnitId == null ? null : getOrganizationUnitById(parentUnitId);
        java.util.Set<Long> visited = new java.util.HashSet<>();
        for (OrganizationUnit cursor = parent; cursor != null; cursor = cursor.getParentUnit()) {
            if (cursor.getTenant() == null || !tenantId.equals(cursor.getTenant().getId()))
                throw new org.springframework.security.access.AccessDeniedException("Parent must belong to the same tenant");
            if (!visited.add(cursor.getId()) || (target.getId() != null && target.getId().equals(cursor.getId())))
                throw new IllegalArgumentException("Organization hierarchy cannot contain a cycle");
        }
        if (target.getId() != null && !java.util.Objects.equals(target.getParentUnitId(), parentUnitId) && !security.isGlobalAdmin())
            governance.requireOwner(tenantId);
        target.setTenant(tenant); target.setParentUnit(parent); target.setName(unit.getName()); target.setUnitCode(unit.getUnitCode());
        target.setUnitType(unit.getUnitType()); target.setCostCenter(unit.getCostCenter()); target.setLocation(unit.getLocation());
        // Boundary flags and child entities are changed only through their authorized operations.
        return orgRepository.save(target);
    }

    @Override
    public void deleteOrganizationUnit(Long id) {
        OrganizationUnit unit = getOrganizationUnitById(id);
        if (!security.isGlobalAdmin()) governance.requireOwner(unit.getTenant().getId());
        orgRepository.deleteById(id);
    }
}
