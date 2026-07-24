package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Set;

import com.mbhoni_creative.admindto.PlanModuleView;

public interface PlanModuleService {

    List<PlanModuleView> getPlanModuleViews(Long planId);

    void updatePlanModules(Long planId, Set<Long> allowedModuleIds);
}