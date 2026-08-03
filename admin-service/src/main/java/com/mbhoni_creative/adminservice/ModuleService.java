package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Set;

import com.mbhoni_creative.admindto.TenantModuleView;
import com.mbhoni_creative.admindto.TenantModulesResponse;
import com.mbhoni_creative.adminentity.PlatformModule;

public interface ModuleService {

    List<PlatformModule> getAllActiveModules();

    List<TenantModuleView> getTenantModuleViews(Long tenantId);

    TenantModulesResponse getTenantModulesResponse(Long tenantId);

    void updateTenantModules(Long tenantId, Set<Long> enabledModuleIds);

    boolean isModuleEnabled(Long tenantId, String moduleCode);

    PlatformModule createModule(PlatformModule module);
}