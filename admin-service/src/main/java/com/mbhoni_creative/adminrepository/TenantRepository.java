package com.mbhoni_creative.adminrepository;

import com.mbhoni_creative.adminentity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);
    
    long countByActive(boolean active);
    
}