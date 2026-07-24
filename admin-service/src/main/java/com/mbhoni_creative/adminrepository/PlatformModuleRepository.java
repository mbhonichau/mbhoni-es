package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.PlatformModule;

public interface PlatformModuleRepository extends JpaRepository<PlatformModule, Long> {

    Optional<PlatformModule> findByCode(String code);

    boolean existsByCode(String code);

    List<PlatformModule> findByActiveTrueOrderByCategoryAscNameAsc();
}