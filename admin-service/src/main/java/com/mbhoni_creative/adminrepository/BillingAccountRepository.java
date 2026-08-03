package com.mbhoni_creative.adminrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.BillingAccount;
import com.mbhoni_creative.adminentity.Tenant;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, Long> {
    Optional<BillingAccount> findByTenant(Tenant tenant);
    Optional<BillingAccount> findByTenantId(Long tenantId);
}