package com.mbhoni_creative.adminservice.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.TenantModuleView;
import com.mbhoni_creative.admindto.TenantModulesResponse;
import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantModule;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.PlatformModuleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanModuleRepository;
import com.mbhoni_creative.adminrepository.TenantModuleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.ModuleService;

@Service
public class ModuleServiceImpl implements ModuleService {

    private final PlatformModuleRepository platformModuleRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionPlanModuleRepository subscriptionPlanModuleRepository;

    public ModuleServiceImpl(
            PlatformModuleRepository platformModuleRepository,
            TenantModuleRepository tenantModuleRepository,
            TenantRepository tenantRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            SubscriptionPlanModuleRepository subscriptionPlanModuleRepository) {

        this.platformModuleRepository = platformModuleRepository;
        this.tenantModuleRepository = tenantModuleRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.subscriptionPlanModuleRepository = subscriptionPlanModuleRepository;
    }

    @Override
    public List<PlatformModule> getAllActiveModules() {
        return platformModuleRepository.findByActiveTrueOrderByCategoryAscNameAsc();
    }

    @Override
    public List<TenantModuleView> getTenantModuleViews(Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        List<PlatformModule> modules = getAllActiveModules();

        List<TenantModule> assignedModules = tenantModuleRepository
                .findByTenantOrderByModuleCategoryAscModuleNameAsc(tenant);

        Set<Long> enabledModuleIds = assignedModules.stream()
                .filter(TenantModule::isEnabled)
                .map(tenantModule -> tenantModule.getModule().getId())
                .collect(Collectors.toSet());

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenant(tenant)
                .orElse(null);

        Set<Long> allowedModuleIds = new HashSet<>();

        if (subscription != null && subscription.getPlan() != null) {
            allowedModuleIds = subscriptionPlanModuleRepository
                    .findByPlanAndAllowedTrue(subscription.getPlan())
                    .stream()
                    .map(planModule -> planModule.getModule().getId())
                    .collect(Collectors.toSet());
        }

        Set<Long> finalAllowedModuleIds = allowedModuleIds;

        return modules.stream()
                .map(module -> {
                    TenantModuleView view = new TenantModuleView();

                    boolean allowedByPlan = finalAllowedModuleIds.contains(module.getId());

                    view.setModuleId(module.getId());
                    view.setCode(module.getCode());
                    view.setName(module.getName());
                    view.setDescription(module.getDescription());
                    view.setCategory(module.getCategory());
                    view.setAllowedByPlan(allowedByPlan);
                    view.setEnabled(enabledModuleIds.contains(module.getId()) && allowedByPlan);

                    return view;
                })
                .collect(Collectors.toList());
    }

    @Override
    public TenantModulesResponse getTenantModulesResponse(Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantModulesResponse response = new TenantModulesResponse();

        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        response.setModules(getTenantModuleViews(tenantId));

        return response;
    }

    @Override
    @Transactional
    public void updateTenantModules(Long tenantId, Set<Long> enabledModuleIds) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Set<Long> safeEnabledIds = enabledModuleIds != null
                ? enabledModuleIds
                : new HashSet<>();

        for (PlatformModule module : getAllActiveModules()) {

            TenantModule tenantModule = tenantModuleRepository
                    .findByTenantAndModule(tenant, module)
                    .orElseGet(() -> {
                        TenantModule created = new TenantModule();
                        created.setTenant(tenant);
                        created.setModule(module);
                        return created;
                    });

            boolean allowedByPlan = isAllowedByCurrentPlan(tenant, module);

            tenantModule.setEnabled(
                    safeEnabledIds.contains(module.getId()) && allowedByPlan
            );

            tenantModuleRepository.save(tenantModule);
        }
    }

    @Override
    public boolean isModuleEnabled(Long tenantId, String moduleCode) {

        if (moduleCode == null || moduleCode.isBlank()) {
            return false;
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        PlatformModule module = platformModuleRepository.findByCode(moduleCode.trim().toUpperCase())
                .orElse(null);

        if (module == null) {
            return false;
        }

        if (!isAllowedByCurrentPlan(tenant, module)) {
            return false;
        }

        return tenantModuleRepository.existsByTenantIdAndModuleCodeAndEnabledTrue(
                tenantId,
                module.getCode()
        );
    }
    
    private boolean isAllowedByCurrentPlan(Tenant tenant, PlatformModule module) {

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenant(tenant)
                .orElse(null);

        if (subscription == null || subscription.getPlan() == null) {
            return false;
        }

        return subscriptionPlanModuleRepository.existsByPlanIdAndModuleCodeAndAllowedTrue(
                subscription.getPlan().getId(),
                module.getCode()
        );
    }
}