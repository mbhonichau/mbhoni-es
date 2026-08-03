package com.mbhoni_creative.admincontroller;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.TenantService;

import com.mbhoni_creative.adminentity.PlatformModule;

@Controller
@RequestMapping("/modules")
public class ModuleController {

    private final ModuleService moduleService;
    private final TenantService tenantService;

    public ModuleController(
            ModuleService moduleService,
            TenantService tenantService) {

        this.moduleService = moduleService;
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MODULE_VIEW')")
    public String index(Model model) {

        model.addAttribute("tenants", tenantService.getAllTenants());

        return "modules/index";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('MODULE_EDIT')")
    public String createModule(
            @ModelAttribute PlatformModule module,
            RedirectAttributes redirectAttributes) {

        try {
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

        model.addAttribute("tenants", tenantService.getAllTenants());
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

        moduleService.updateTenantModules(tenantId, enabledModuleIds);

        redirectAttributes.addFlashAttribute("successMessage", "Tenant modules updated successfully.");

        return "redirect:/modules";
    }
}