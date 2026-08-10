package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminservice.EmployeeService;
import com.mbhoni_creative.config.TenantAccessService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;
    private final TenantAccessService tenantAccessService;

    public EmployeeApiController(EmployeeService employeeService, TenantAccessService tenantAccessService) {
        this.employeeService = employeeService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('API_KEY'))")
    public ResponseEntity<List<Employee>> getEmployees(@RequestParam(required = false) Long tenantId) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(employeeService.getEmployeesByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('API_KEY'))")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        tenantAccessService.requireAccess(employee.getTenant() != null ? employee.getTenant().getId() : null);
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and hasAuthority('EMPLOYEE_EDIT')")
    public ResponseEntity<Employee> createEmployee(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long managerId,
            @RequestBody Employee employee) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(employeeService.saveEmployee(tenantId, orgUnitId, managerId, employee));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and hasAuthority('EMPLOYEE_EDIT')")
    public ResponseEntity<Employee> updateStatus(@PathVariable Long id, @RequestParam EmployeeStatus status) {
        Employee employee = employeeService.getEmployeeById(id);
        tenantAccessService.requireAccess(employee.getTenant() != null ? employee.getTenant().getId() : null);
        return ResponseEntity.ok(employeeService.updateEmployeeStatus(id, status));
    }
}
