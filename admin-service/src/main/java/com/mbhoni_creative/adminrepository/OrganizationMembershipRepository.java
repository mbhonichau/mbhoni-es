package com.mbhoni_creative.adminrepository;

import com.mbhoni_creative.adminentity.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    List<OrganizationMembership> findByUserIdAndUnitTenantId(Long userId, Long tenantId);
    Optional<OrganizationMembership> findByUserIdAndUnitId(Long userId, Long unitId);
}
