package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mbhoni_creative.adminentity.Employee;
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
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String index(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        model.addAttribute("employees", employeeService.getEmployeesByTenant(tenantId));
        model.addAttribute("tenants", tenantService.getAllTenants());

        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());
        if (effectiveTenantId != null) {
            model.addAttribute("orgUnits", orgUnitService.getOrganizationUnitsByTenant(effectiveTenantId));
        }

        model.addAttribute("newEmployee", new Employee());
        return "employees/list";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveEmployee(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long managerId,
            @ModelAttribute Employee employee) {
        Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
        employeeService.saveEmployee(targetTenantId, orgUnitId, managerId, employee);
        return "redirect:/employees";
    }
}
