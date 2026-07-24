package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.OrganizationUnit;

@Repository
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {
    List<OrganizationUnit> findByTenantId(Long tenantId);
    Optional<OrganizationUnit> findByTenantIdAndUnitCode(Long tenantId, String unitCode);
    List<OrganizationUnit> findByTenantIdAndParentUnitIsNull(Long tenantId);
}
