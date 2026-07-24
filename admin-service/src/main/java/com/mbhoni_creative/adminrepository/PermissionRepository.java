package com.mbhoni_creative.adminrepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Permission;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);
    
    List<Permission> findByNameIn(Collection<String> names);
}