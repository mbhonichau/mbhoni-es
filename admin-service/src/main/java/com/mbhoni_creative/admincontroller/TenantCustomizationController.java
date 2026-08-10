package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminentity.TenantCustomization;
import com.mbhoni_creative.adminentity.ThemeMode;
import com.mbhoni_creative.adminservice.TenantCustomizationService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.config.TenantAccessService;

@Controller
@RequestMapping("/customizations")
public class TenantCustomizationController {

    private final TenantCustomizationService customizationService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;
    private final TenantAccessService tenantAccessService;

    public TenantCustomizationController(
            TenantCustomizationService customizationService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService,
            TenantAccessService tenantAccessService) {

        this.customizationService = customizationService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    @PreAuthorize("@tenantEntitlementService.isFeatureEnabled('BRANDING') and hasAuthority('CUSTOMIZATION_VIEW')")
    public String list(Model model) {

        if (!tenantSecurityService.isGlobalAdmin()) {
            return "redirect:/customizations/my";
        }

        model.addAttribute("tenants", tenantService.getAllTenants());

        return "customizations/list";
    }

    @PostMapping("/edit")
    @PreAuthorize("@tenantEntitlementService.isFeatureEnabled('BRANDING') and hasAuthority('CUSTOMIZATION_VIEW')")
    public String edit(
            @RequestParam Long tenantId,
            Model model) {

        tenantAccessService.requireAccess(tenantId);
        model.addAttribute("customization", customizationService.getOrCreateForTenant(tenantId));
        model.addAttribute("brandingAllowed", customizationService.brandingAllowed(tenantId));
        model.addAttribute("themeModes", ThemeMode.values());
        model.addAttribute("saveAction", "/customizations/update");

        return "customizations/edit";
    }

    @PostMapping("/update")
    @PreAuthorize("@tenantEntitlementService.isFeatureEnabled('BRANDING') and hasAuthority('CUSTOMIZATION_EDIT')")
    public String update(
            @RequestParam Long tenantId,
            @ModelAttribute TenantCustomization customization,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(tenantId);
        customizationService.saveForTenant(tenantId, customization);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant customization saved successfully.");

        return "redirect:/customizations";
    }

    @GetMapping("/my")
    @PreAuthorize("@tenantEntitlementService.isFeatureEnabled('BRANDING') and hasAuthority('CUSTOMIZATION_VIEW')")
    public String myCustomization(Model model) {

        TenantCustomization customization = customizationService.getOrCreateForCurrentTenant();

        model.addAttribute("customization", customization);
        model.addAttribute("brandingAllowed", customizationService.brandingAllowed(customization.getTenant().getId()));
        model.addAttribute("themeModes", ThemeMode.values());
        model.addAttribute("saveAction", "/customizations/my/update");

        return "customizations/edit";
    }

    @PostMapping("/my/update")
    @PreAuthorize("@tenantEntitlementService.isFeatureEnabled('BRANDING') and hasAuthority('CUSTOMIZATION_EDIT')")
    public String updateMyCustomization(
            @ModelAttribute TenantCustomization customization,
            RedirectAttributes redirectAttributes) {

        customizationService.saveForCurrentTenant(customization);
        redirectAttributes.addFlashAttribute("successMessage", "Branding saved successfully.");

        return "redirect:/customizations/my";
    }
}
