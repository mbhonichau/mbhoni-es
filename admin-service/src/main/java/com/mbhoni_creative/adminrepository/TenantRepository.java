package com.mbhoni_creative.adminrepository;

import com.mbhoni_creative.adminentity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);
    
    long countByActive(boolean active);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Tenant t where t.id = :id")
    Optional<Tenant> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    
}
