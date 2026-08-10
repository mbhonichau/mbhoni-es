package com.mbhoni_creative.admincontroller;

import java.util.Set;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.TenantService;

import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.config.TenantAccessService;

@Controller
@RequestMapping("/modules")
public class ModuleController {

    private final ModuleService moduleService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;
    private final TenantAccessService tenantAccessService;

    public ModuleController(
            ModuleService moduleService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService,
            TenantAccessService tenantAccessService) {

        this.moduleService = moduleService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MODULE_VIEW')")
    public String index(Model model) {

        model.addAttribute("tenants", getAccessibleTenants());

        return "modules/index";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('MODULE_EDIT')")
    public String createModule(
            @ModelAttribute PlatformModule module,
            RedirectAttributes redirectAttributes) {

        try {
            enforceGlobalAdmin();
            moduleService.createModule(module);
            redirectAttributes.addFlashAttribute("successMessage", "Platform module '" + module.getName() + "' created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/modules";
    }

    @PostMapping("/tenant")
    @PreAuthorize("hasAuthority('MODULE_VIEW')")
    public String tenantModules(
            @RequestParam Long tenantId,
            Model model) {

        tenantAccessService.requireAccess(tenantId);
        model.addAttribute("tenants", getAccessibleTenants());
        model.addAttribute("selectedTenantId", tenantId);
        model.addAttribute("modules", moduleService.getTenantModuleViews(tenantId));

        return "modules/index";
    }

    @PostMapping("/tenant/update")
    @PreAuthorize("hasAuthority('MODULE_EDIT')")
    public String updateTenantModules(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Set<Long> enabledModuleIds,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(tenantId);
        moduleService.updateTenantModules(tenantId, enabledModuleIds);

        redirectAttributes.addFlashAttribute("successMessage", "Tenant modules updated successfully.");

        return "redirect:/modules";
    }

    private void enforceGlobalAdmin() {
        if (!tenantSecurityService.isGlobalAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: global admin only");
        }
    }

    private List<com.mbhoni_creative.admindto.TenantDto> getAccessibleTenants() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return tenantService.getAllTenants();
        }

        Long tenantId = tenantAccessService.getCurrentTenantId();
        return tenantId == null ? List.of() : List.of(tenantService.getTenantById(tenantId));
    }
}
