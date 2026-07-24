package com.mbhoni_creative.admincontroller;

import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.ModuleCheckResponse;
import com.mbhoni_creative.admindto.TenantModulesResponse;
import com.mbhoni_creative.adminservice.ModuleService;

@RestController
@RequestMapping("/api/public/modules")
public class PublicModuleController {

    private final ModuleService moduleService;

    public PublicModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/{tenantId}")
    public TenantModulesResponse getTenantModules(
            @PathVariable Long tenantId) {

        return moduleService.getTenantModulesResponse(tenantId);
    }

    @GetMapping("/{tenantId}/check/{moduleCode}")
    public ModuleCheckResponse checkModule(
            @PathVariable Long tenantId,
            @PathVariable String moduleCode) {

        return new ModuleCheckResponse(
                tenantId,
                moduleCode.toUpperCase(),
                moduleService.isModuleEnabled(tenantId, moduleCode)
        );
    }
}