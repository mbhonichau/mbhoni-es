package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    List<User> findByTenant(Tenant tenant);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    
    long countByTenant(Tenant tenant);
    
    long countByTenantId(Long tenantId);
}
