package com.mbhoni_creative.adminrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantCustomization;

public interface TenantCustomizationRepository extends JpaRepository<TenantCustomization, Long> {

    Optional<TenantCustomization> findByTenant(Tenant tenant);

    Optional<TenantCustomization> findByTenantId(Long tenantId);
    
    Optional<TenantCustomization> findByCustomDomainIgnoreCase(String customDomain);
}