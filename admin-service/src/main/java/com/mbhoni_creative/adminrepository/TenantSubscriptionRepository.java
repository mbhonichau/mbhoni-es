package com.mbhoni_creative.adminrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findByTenant(Tenant tenant);
    
    Optional<TenantSubscription> findByTenantId(Long tenantId);
}