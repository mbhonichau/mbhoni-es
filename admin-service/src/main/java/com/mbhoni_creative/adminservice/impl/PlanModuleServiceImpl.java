package com.mbhoni_creative.adminservice.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.PlanModuleView;
import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionPlanModule;
import com.mbhoni_creative.adminrepository.PlatformModuleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanModuleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanRepository;
import com.mbhoni_creative.adminservice.PlanModuleService;

@Service
public class PlanModuleServiceImpl implements PlanModuleService {

    private final SubscriptionPlanRepository planRepository;
    private final PlatformModuleRepository moduleRepository;
    private final SubscriptionPlanModuleRepository planModuleRepository;

    public PlanModuleServiceImpl(
            SubscriptionPlanRepository planRepository,
            PlatformModuleRepository moduleRepository,
            SubscriptionPlanModuleRepository planModuleRepository) {

        this.planRepository = planRepository;
        this.moduleRepository = moduleRepository;
        this.planModuleRepository = planModuleRepository;
    }

    @Override
    public List<PlanModuleView> getPlanModuleViews(Long planId) {

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        Set<Long> allowedModuleIds = planModuleRepository.findByPlanAndAllowedTrue(plan)
                .stream()
                .map(planModule -> planModule.getModule().getId())
                .collect(Collectors.toSet());

        return moduleRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                .stream()
                .map(module -> {
                    PlanModuleView view = new PlanModuleView();

                    view.setModuleId(module.getId());
                    view.setCode(module.getCode());
                    view.setName(module.getName());
                    view.setDescription(module.getDescription());
                    view.setCategory(module.getCategory());
                    view.setAllowed(allowedModuleIds.contains(module.getId()));

                    return view;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updatePlanModules(Long planId, Set<Long> allowedModuleIds) {

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        Set<Long> safeAllowedIds = allowedModuleIds != null
                ? allowedModuleIds
                : new HashSet<>();

        for (PlatformModule module : moduleRepository.findByActiveTrueOrderByCategoryAscNameAsc()) {

            SubscriptionPlanModule planModule = planModuleRepository
                    .findByPlanAndModule(plan, module)
                    .orElseGet(() -> {
                        SubscriptionPlanModule created = new SubscriptionPlanModule();
                        created.setPlan(plan);
                        created.setModule(module);
                        return created;
                    });

            planModule.setAllowed(safeAllowedIds.contains(module.getId()));

            planModuleRepository.save(planModule);
        }
    }
}