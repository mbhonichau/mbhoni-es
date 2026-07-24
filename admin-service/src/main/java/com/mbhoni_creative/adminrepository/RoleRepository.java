package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.Tenant;


public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findByNameAndTenantIsNull(String name);

    List<Role> findByTenant(Tenant tenant);
    
    Optional<Role> findByNameAndTenantId(String name, Long tenantId);

    List<Role> findByTenantIdOrderByNameAsc(Long tenantId);

    List<Role> findByTenantIsNullOrderByNameAsc();

    List<Role> findByIdIn(Collection<Long> ids);
}
