package com.mbhoni_creative.admincontroller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.OnboardingSchemaResponse;
import com.mbhoni_creative.admindto.TenantOnboardingFieldDto;
import com.mbhoni_creative.adminentity.RequirementState;
import com.mbhoni_creative.adminentity.TargetEntity;
import com.mbhoni_creative.adminservice.TenantOnboardingFieldService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantAccessService;

@Controller
@RequestMapping("/tenants")
public class TenantOnboardingFieldController {

    private final TenantOnboardingFieldService onboardingFieldService;
    private final TenantService tenantService;
    private final TenantAccessService tenantAccessService;

    public TenantOnboardingFieldController(
            TenantOnboardingFieldService onboardingFieldService,
            TenantService tenantService,
            TenantAccessService tenantAccessService) {

        this.onboardingFieldService = onboardingFieldService;
        this.tenantService = tenantService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/{id}/onboarding-fields")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String viewOnboardingFields(
            @PathVariable Long id,
            Model model) {

        tenantAccessService.requireAccess(id);

        OnboardingSchemaResponse schema = onboardingFieldService.getOnboardingSchemaForTenant(id);

        model.addAttribute("schema", schema);
        model.addAttribute("tenant", tenantService.getTenantById(id));
        model.addAttribute("requirementStates", RequirementState.values());
        model.addAttribute("targetEntities", TargetEntity.values());
        model.addAttribute("saveAction", "/tenants/" + id + "/onboarding-fields");

        return "tenants/onboarding-fields";
    }

    @PostMapping("/{id}/onboarding-fields")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String saveOnboardingFields(
            @PathVariable Long id,
            @ModelAttribute("fieldForm") FieldFormWrapper form,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(id);

        if (form.getFields() != null && !form.getFields().isEmpty()) {
            onboardingFieldService.saveOrUpdateFields(id, form.getFields());
        }

        redirectAttributes.addFlashAttribute("successMessage", "Onboarding and registration field requirements updated successfully.");

        return "redirect:/tenants/" + id + "/onboarding-fields";
    }

    @PostMapping("/{id}/onboarding-fields/add-custom")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String addCustomField(
            @PathVariable Long id,
            @ModelAttribute TenantOnboardingFieldDto customFieldDto,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(id);

        try {
            onboardingFieldService.addCustomField(id, customFieldDto);
            redirectAttributes.addFlashAttribute("successMessage", "Custom section field defined and added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error defining custom field: " + e.getMessage());
        }

        return "redirect:/tenants/" + id + "/onboarding-fields";
    }

    @PostMapping("/{id}/onboarding-fields/delete")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String deleteField(
            @PathVariable Long id,
            @RequestParam Long fieldId,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(id);

        try {
            onboardingFieldService.deleteField(id, fieldId);
            redirectAttributes.addFlashAttribute("successMessage", "Field configuration removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting field: " + e.getMessage());
        }

        return "redirect:/tenants/" + id + "/onboarding-fields";
    }

    public static class FieldFormWrapper {
        private List<TenantOnboardingFieldDto> fields;

        public List<TenantOnboardingFieldDto> getFields() { return fields; }
        public void setFields(List<TenantOnboardingFieldDto> fields) { this.fields = fields; }
    }
}
