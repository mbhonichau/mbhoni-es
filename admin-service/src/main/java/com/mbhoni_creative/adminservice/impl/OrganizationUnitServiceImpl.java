package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.OrganizationUnitRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.OrganizationUnitService;

@Service
@Transactional
public class OrganizationUnitServiceImpl implements OrganizationUnitService {

    private final OrganizationUnitRepository orgRepository;
    private final TenantRepository tenantRepository;

    public OrganizationUnitServiceImpl(
            OrganizationUnitRepository orgRepository,
            TenantRepository tenantRepository) {
        this.orgRepository = orgRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationUnit> getOrganizationUnitsByTenant(Long tenantId) {
        return orgRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationUnit> getRootOrganizationUnits(Long tenantId) {
        return orgRepository.findByTenantIdAndParentUnitIsNull(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationUnit getOrganizationUnitById(Long id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization Unit not found with ID: " + id));
    }

    @Override
    public OrganizationUnit saveOrganizationUnit(Long tenantId, Long parentUnitId, OrganizationUnit unit) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
        unit.setTenant(tenant);

        if (parentUnitId != null) {
            OrganizationUnit parent = getOrganizationUnitById(parentUnitId);
            unit.setParentUnit(parent);
        }

        return orgRepository.save(unit);
    }

    @Override
    public void deleteOrganizationUnit(Long id) {
        orgRepository.deleteById(id);
    }
}
