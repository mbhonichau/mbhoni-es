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
    public String index(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());

        if (effectiveTenantId != null) {
            model.addAttribute("orgUnits", orgUnitService.getOrganizationUnitsByTenant(effectiveTenantId));
        }

        model.addAttribute("tenants", getAccessibleTenants());
        model.addAttribute("newUnit", new OrganizationUnit());
        return "organization/list";
    }

    @PostMapping("/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('ORG') and (hasAuthority('ORG_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String saveUnit(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long parentUnitId,
            @ModelAttribute OrganizationUnit unit) {
        Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
        if (!tenantSecurityService.isGlobalAdmin()) {
            Long currentTenantId = tenantSecurityService.getCurrentTenantId();
            if (currentTenantId != null) {
                targetTenantId = currentTenantId;
            }
        }

        orgUnitService.saveOrganizationUnit(targetTenantId, parentUnitId, unit);
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
