package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.OrganizationUnit;

public interface OrganizationUnitService {
    List<OrganizationUnit> getOrganizationUnitsByTenant(Long tenantId);
    List<OrganizationUnit> getRootOrganizationUnits(Long tenantId);
    OrganizationUnit getOrganizationUnitById(Long id);
    OrganizationUnit saveOrganizationUnit(Long tenantId, Long parentUnitId, OrganizationUnit unit);
    void deleteOrganizationUnit(Long id);
}
