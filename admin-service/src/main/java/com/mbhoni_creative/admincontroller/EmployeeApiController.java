package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminservice.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;

    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<Employee>> getEmployees(@RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(employeeService.getEmployeesByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    public ResponseEntity<Employee> createEmployee(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long managerId,
            @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.saveEmployee(tenantId, orgUnitId, managerId, employee));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    public ResponseEntity<Employee> updateStatus(@PathVariable Long id, @RequestParam EmployeeStatus status) {
        return ResponseEntity.ok(employeeService.updateEmployeeStatus(id, status));
    }
}
