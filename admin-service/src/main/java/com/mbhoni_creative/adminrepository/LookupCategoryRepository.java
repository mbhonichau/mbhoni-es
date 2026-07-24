package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.LookupCategory;

@Repository
public interface LookupCategoryRepository extends JpaRepository<LookupCategory, Long> {
    Optional<LookupCategory> findByCode(String code);
    List<LookupCategory> findByTenantIdOrTenantIsNull(Long tenantId);
}
