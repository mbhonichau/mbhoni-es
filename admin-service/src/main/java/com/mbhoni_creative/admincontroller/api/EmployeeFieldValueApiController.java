package com.mbhoni_creative.admincontroller.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.EmployeeCompletenessReportDto;
import com.mbhoni_creative.adminservice.EmployeeFieldValueService;
import com.mbhoni_creative.adminservice.EmployeeService;
import com.mbhoni_creative.config.TenantSecurityService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeFieldValueApiController {

    private final EmployeeFieldValueService fieldValueService;
    private final EmployeeService employeeService;
    private final TenantSecurityService tenantSecurityService;

    public EmployeeFieldValueApiController(
            EmployeeFieldValueService fieldValueService,
            EmployeeService employeeService,
            TenantSecurityService tenantSecurityService) {
        this.fieldValueService = fieldValueService;
        this.employeeService = employeeService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping("/{id}/field-values")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public ResponseEntity<Map<String, String>> getFieldValues(@PathVariable Long id) {
        Map<String, String> values = fieldValueService.getValueMapForEmployee(id);
        return ResponseEntity.ok(values);
    }

    @PutMapping("/{id}/field-values")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public ResponseEntity<Map<String, String>> saveFieldValues(@PathVariable Long id, @RequestBody Map<String, String> fieldValues) {
        fieldValueService.saveValues(id, fieldValues);
        Map<String, String> updatedValues = fieldValueService.getValueMapForEmployee(id);
        return ResponseEntity.ok(updatedValues);
    }

    @GetMapping("/completeness-report")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_COMPLETENESS_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('EMPLOYEE_VIEW'))")
    public ResponseEntity<List<EmployeeCompletenessReportDto>> getCompletenessReport() {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        List<EmployeeCompletenessReportDto> report = employeeService.getCompletenessReport(tenantId);
        return ResponseEntity.ok(report);
    }
}
