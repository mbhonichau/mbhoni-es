package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminentity.FieldDataType;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantAccessService;

@Controller
public class FieldDefinitionController {

    private final FieldDefinitionService fieldDefinitionService;
    private final TenantService tenantService;
    private final TenantAccessService tenantAccessService;

    public FieldDefinitionController(
            FieldDefinitionService fieldDefinitionService,
            TenantService tenantService,
            TenantAccessService tenantAccessService) {
        this.fieldDefinitionService = fieldDefinitionService;
        this.tenantService = tenantService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/employee-fields")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String currentTenantEmployeeFields() {
        Long tenantId = tenantAccessService.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = tenantService.getAllTenants().isEmpty() ? 1L : tenantService.getAllTenants().get(0).getId();
        }
        return "redirect:/tenants/" + tenantId + "/employee-fields";
    }

    @GetMapping("/tenants/{tenantId}/employee-fields")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_EDIT')")
    public String viewEmployeeFields(
            @PathVariable Long tenantId,
            Model model) {

        tenantAccessService.requireAccess(tenantId);

        model.addAttribute("tenant", tenantService.getTenantById(tenantId));
        model.addAttribute("sections", FieldSection.values());
        model.addAttribute("dataTypes", FieldDataType.values());

        model.addAttribute("qualificationFields", fieldDefinitionService.getEffectiveSchema(tenantId, FieldSection.QUALIFICATION));
        model.addAttribute("backgroundCheckFields", fieldDefinitionService.getEffectiveSchema(tenantId, FieldSection.BACKGROUND_CHECK));
        model.addAttribute("employmentStatusFields", fieldDefinitionService.getEffectiveSchema(tenantId, FieldSection.EMPLOYMENT_STATUS));
        model.addAttribute("fieldAssignmentFields", fieldDefinitionService.getEffectiveSchema(tenantId, FieldSection.FIELD_ASSIGNMENT));

        model.addAttribute("newField", new FieldDefinition());
        model.addAttribute("saveAction", "/tenants/" + tenantId + "/employee-fields/create");

        return "tenants/employee-fields";
    }

    @PostMapping("/{tenantId}/employee-fields/create")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_EDIT')")
    public String createField(
            @PathVariable Long tenantId,
            @ModelAttribute FieldDefinition definition,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(tenantId);

        try {
            fieldDefinitionService.createField(tenantId, definition);
            redirectAttributes.addFlashAttribute("successMessage", "Field definition '" + definition.getLabel() + "' created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating field definition: " + e.getMessage());
        }

        return "redirect:/tenants/" + tenantId + "/employee-fields";
    }

    @PostMapping("/{tenantId}/employee-fields/{id}/update")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_EDIT')")
    public String updateField(
            @PathVariable Long tenantId,
            @PathVariable Long id,
            @ModelAttribute FieldDefinition definition,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(tenantId);

        try {
            fieldDefinitionService.updateField(tenantId, id, definition);
            redirectAttributes.addFlashAttribute("successMessage", "Field definition updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating field definition: " + e.getMessage());
        }

        return "redirect:/tenants/" + tenantId + "/employee-fields";
    }

    @PostMapping("/{tenantId}/employee-fields/{id}/deactivate")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_EDIT')")
    public String deactivateField(
            @PathVariable Long tenantId,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        tenantAccessService.requireAccess(tenantId);

        try {
            fieldDefinitionService.deactivateField(tenantId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Field definition deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deactivating field: " + e.getMessage());
        }

        return "redirect:/tenants/" + tenantId + "/employee-fields";
    }
}
