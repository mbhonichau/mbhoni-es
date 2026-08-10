package com.mbhoni_creative.admincontroller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminservice.OrganizationUnitService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/organization")
public class OrganizationUnitViewController {

    private final OrganizationUnitService orgUnitService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;

    public OrganizationUnitViewController(
            OrganizationUnitService orgUnitService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService) {
        this.orgUnitService = orgUnitService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('ORG') and (hasAuthority('ORG_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String index(
            @RequestParam(required = false) Long tenantId,
            Model model) {
        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        List<TenantDto> accessibleTenants = getAccessibleTenants();

        Long effectiveTenantId = tenantId != null ? tenantId : currentTenantId;
        if (effectiveTenantId == null && !accessibleTenants.isEmpty()) {
            effectiveTenantId = accessibleTenants.get(0).getId();
        }

        List<OrganizationUnit> orgUnits = orgUnitService.getOrganizationUnitsByTenant(effectiveTenantId);
        List<OrganizationUnit> parentUnits = (effectiveTenantId != null)
                ? orgUnits
                : orgUnitService.getOrganizationUnitsByTenant(null);

        model.addAttribute("orgUnits", orgUnits);
        model.addAttribute("parentUnits", parentUnits);
        model.addAttribute("tenants", accessibleTenants);
        model.addAttribute("selectedTenantId", effectiveTenantId);
        model.addAttribute("newUnit", new OrganizationUnit());
        return "organization/list";
    }

    @PostMapping("/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('ORG') and (hasAuthority('ORG_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String saveUnit(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long parentUnitId,
            @ModelAttribute OrganizationUnit unit,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
            if (!tenantSecurityService.isGlobalAdmin()) {
                Long currentTenantId = tenantSecurityService.getCurrentTenantId();
                if (currentTenantId != null) {
                    targetTenantId = currentTenantId;
                }
            }
            if (targetTenantId == null) {
                List<TenantDto> accessibleTenants = getAccessibleTenants();
                if (!accessibleTenants.isEmpty()) {
                    targetTenantId = accessibleTenants.get(0).getId();
                }
            }

            if (targetTenantId == null) {
                throw new IllegalArgumentException("Target Tenant ID is required to create an Organization Unit.");
            }

            orgUnitService.saveOrganizationUnit(targetTenantId, parentUnitId, unit);
            redirectAttributes.addFlashAttribute("successMessage", "Organization Unit '" + unit.getName() + "' saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving Organization Unit: " + e.getMessage());
        }

        return "redirect:/organization";
    }

    private List<TenantDto> getAccessibleTenants() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return tenantService.getAllTenants();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        try {
            return List.of(tenantService.getTenantById(tenantId));
        } catch (Exception e) {
            return List.of();
        }
    }
}
