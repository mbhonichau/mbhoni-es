package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionPlanModule;

public interface SubscriptionPlanModuleRepository extends JpaRepository<SubscriptionPlanModule, Long> {

    List<SubscriptionPlanModule> findByPlanAndAllowedTrue(SubscriptionPlan plan);

    Optional<SubscriptionPlanModule> findByPlanAndModule(SubscriptionPlan plan, PlatformModule module);

    boolean existsByPlanIdAndModuleCodeAndAllowedTrue(Long planId, String moduleCode);
}