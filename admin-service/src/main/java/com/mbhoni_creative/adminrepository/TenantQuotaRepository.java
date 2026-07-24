package com.mbhoni_creative.adminrepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.TenantQuota;

@Repository
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, Long> {
    Optional<TenantQuota> findByTenantId(Long tenantId);
}
