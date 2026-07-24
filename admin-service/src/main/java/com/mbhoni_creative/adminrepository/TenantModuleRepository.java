package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantModule;

public interface TenantModuleRepository extends JpaRepository<TenantModule, Long> {

    List<TenantModule> findByTenantOrderByModuleCategoryAscModuleNameAsc(Tenant tenant);

    Optional<TenantModule> findByTenantAndModule(Tenant tenant, PlatformModule module);

    boolean existsByTenantIdAndModuleCodeAndEnabledTrue(Long tenantId, String moduleCode);
}