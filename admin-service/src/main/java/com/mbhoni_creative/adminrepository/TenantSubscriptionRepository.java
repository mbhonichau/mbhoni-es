package com.mbhoni_creative.adminrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findByTenant(Tenant tenant);
    
    Optional<TenantSubscription> findByTenantId(Long tenantId);

    Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from TenantSubscription s where s.tenant.id = :tenantId")
    Optional<TenantSubscription> lockByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    boolean existsByPlanId(Long planId);

    @org.springframework.data.jpa.repository.Query("select s.tenant.id from TenantSubscription s where s.stripeSubscriptionId is not null")
    java.util.List<Long> findStripeLinkedTenantIds();

    @org.springframework.data.jpa.repository.Query("select s.tenant.id from TenantSubscription s where s.graceEndsAt is not null or s.status = com.mbhoni_creative.adminentity.SubscriptionStatus.TRIAL")
    java.util.List<Long> findLifecycleCandidateTenantIds();

    @org.springframework.data.jpa.repository.Query("select s.tenant.id from TenantSubscription s where s.status = com.mbhoni_creative.adminentity.SubscriptionStatus.CANCELLED and s.retentionEndsAt <= :now")
    java.util.List<Long> findExpiredRetentionTenantIds(@org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
