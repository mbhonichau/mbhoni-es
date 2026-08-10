package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.TenantSettingsDto;
import com.mbhoni_creative.adminservice.TenantSettingsService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantAccessService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/tenants")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;
    private final TenantService tenantService;
    private final TenantAccessService tenantAccessService;
    private final TenantSecurityService tenantSecurityService;

    public TenantSettingsController(
            TenantSettingsService tenantSettingsService,
            TenantService tenantService,
            TenantAccessService tenantAccessService,
            TenantSecurityService tenantSecurityService) {

        this.tenantSettingsService = tenantSettingsService;
        this.tenantService = tenantService;
        this.tenantAccessService = tenantAccessService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping("/{id}/settings")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String viewSettings(
            @PathVariable Long id,
            Model model) {

        tenantAccessService.requireAccess(id);

        TenantSettingsDto settings = tenantSettingsService.getSettingsForTenant(id);
        model.addAttribute("settings", settings);
        model.addAttribute("tenant", tenantService.getTenantById(id));
        model.addAttribute("saveAction", "/tenants/" + id + "/settings");

        return "tenants/settings";
    }

    @PostMapping("/{id}/settings")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String updateSettings(
            @PathVariable Long id,
            @ModelAttribute TenantSettingsDto settingsDto,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(id);

        tenantSettingsService.updateSettingsForTenant(id, settingsDto);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant administration settings updated successfully.");

        return "redirect:/tenants/" + id + "/settings";
    }

    @GetMapping("/settings/my")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String viewMySettings(Model model) {

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return "redirect:/dashboard";
        }

        return viewSettings(tenantId, model);
    }

    @PostMapping("/settings/my")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String updateMySettings(
            @ModelAttribute TenantSettingsDto settingsDto,
            RedirectAttributes redirectAttributes) {

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return "redirect:/dashboard";
        }

        return updateSettings(tenantId, settingsDto, redirectAttributes);
    }
}
