package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantApiKey;

public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, Long> {

    List<TenantApiKey> findByTenantOrderByCreatedAtDesc(Tenant tenant);

    List<TenantApiKey> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<TenantApiKey> findByKeyHash(String keyHash);
}
