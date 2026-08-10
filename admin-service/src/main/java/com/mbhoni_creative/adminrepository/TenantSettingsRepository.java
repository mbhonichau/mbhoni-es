package com.mbhoni_creative.adminrepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.TenantSettings;

@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {
    Optional<TenantSettings> findByTenantId(Long tenantId);
}
