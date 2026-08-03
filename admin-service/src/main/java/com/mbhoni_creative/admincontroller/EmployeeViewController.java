package com.mbhoni_creative.admincontroller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminservice.EmployeeService;
import com.mbhoni_creative.adminservice.OrganizationUnitService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/employees")
public class EmployeeViewController {

    private final EmployeeService employeeService;
    private final OrganizationUnitService orgUnitService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;

    public EmployeeViewController(
            EmployeeService employeeService,
            OrganizationUnitService orgUnitService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService) {
        this.employeeService = employeeService;
        this.orgUnitService = orgUnitService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String index(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        model.addAttribute("employees", employeeService.getEmployeesByTenant(tenantId));
        model.addAttribute("tenants", getAccessibleTenants());
        model.addAttribute("statuses", EmployeeStatus.values());

        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());
        if (effectiveTenantId != null) {
            model.addAttribute("orgUnits", orgUnitService.getOrganizationUnitsByTenant(effectiveTenantId));
            model.addAttribute("managers", employeeService.getEmployeesByTenant(effectiveTenantId));
        }

        model.addAttribute("newEmployee", new Employee());
        return "employees/list";
    }

    @PostMapping("/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String saveEmployee(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long managerId,
            @ModelAttribute Employee employee) {
        Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
        
        if (!tenantSecurityService.isGlobalAdmin()) {
            Long currentTenantId = tenantSecurityService.getCurrentTenantId();
            if (currentTenantId != null) {
                targetTenantId = currentTenantId;
            }
        }
        
        employeeService.saveEmployee(targetTenantId, orgUnitId, managerId, employee);
        return "redirect:/employees";
    }

    @PostMapping("/status")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String updateStatus(
            @RequestParam Long id,
            @RequestParam EmployeeStatus status) {
        
        Employee employee = employeeService.getEmployeeById(id);
        if (!tenantSecurityService.isGlobalAdmin()) {
            Long currentTenantId = tenantSecurityService.getCurrentTenantId();
            if (employee.getTenant() == null || !employee.getTenant().getId().equals(currentTenantId)) {
                throw new RuntimeException("Access denied");
            }
        }

        employeeService.updateEmployeeStatus(id, status);
        return "redirect:/employees";
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
